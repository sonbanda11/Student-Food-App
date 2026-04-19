package com.example.studentfood.data.remote.manager;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.studentfood.data.remote.api.OverpassPlacesApiService;
import com.example.studentfood.data.remote.client.RetrofitClient;
import com.example.studentfood.data.remote.dto.OverpassResponse;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Production-ready request manager for Overpass API
 * Implements single-flight requests, throttling, and exponential backoff
 * Prevents HTTP 429 rate limiting through intelligent request control
 */
public class OverpassRequestManager {
    
    private static final String TAG = "OverpassRequestManager";
    private static final int MAX_RETRIES = 3;
    
    // Rate limiting constants
    private static final long MIN_REQUEST_INTERVAL_MS = 5000; // 5 seconds minimum between requests
    private static final long RATE_LIMIT_BACKOFF_INITIAL_MS = 5000; // 5 seconds initial backoff
    private static final long RATE_LIMIT_BACKOFF_MAX_MS = 60000; // 60 seconds max backoff
    private static final long RETRY_DELAY_BASE_MS = 2000; // 2 seconds base retry delay
    
    // Single instance
    private static OverpassRequestManager instance;
    private final OverpassPlacesApiService apiService;
    private final Handler mainHandler;
    
    // Request control
    private final Map<String, ActiveRequest> activeRequests = new ConcurrentHashMap<>();
    private volatile long lastSuccessfulRequestTime = 0;
    private volatile long rateLimitBackoffUntil = 0;
    private final AtomicBoolean isGlobalRequestInProgress = new AtomicBoolean(false);
    
    // Request deduplication and queuing
    private final List<QueuedCallback> pendingRequests = new ArrayList<>();
    
    private OverpassRequestManager() {
        this.apiService = RetrofitClient.getOverpassInstance().create(OverpassPlacesApiService.class);
        this.mainHandler = new Handler(Looper.getMainLooper());
    }
    
    public static synchronized OverpassRequestManager getInstance() {
        if (instance == null) {
            instance = new OverpassRequestManager();
        }
        return instance;
    }
    
    /**
     * Execute request with production-grade rate limiting and single-flight control
     * @param query Overpass QL query
     * @param requestKey Unique key for request deduplication
     * @param callback Response callback
     */
    public void executeRequest(String query, String requestKey, RequestCallback callback) {
        Log.d(TAG, "=== REQUEST MANAGER: executeRequest ===");
        Log.d(TAG, "RequestKey: " + requestKey);
        Log.d(TAG, "Active requests: " + activeRequests.size());
        synchronized (pendingRequests) {
            Log.d(TAG, "Pending requests in queue: " + pendingRequests.size());
        }
        
        // Check if we're in rate limit backoff
        long currentTime = System.currentTimeMillis();
        if (currentTime < rateLimitBackoffUntil) {
            long remainingBackoff = rateLimitBackoffUntil - currentTime;
            Log.w(TAG, "In rate limit backoff, retrying in " + remainingBackoff + "ms");
            scheduleRetry(query, requestKey, callback, remainingBackoff);
            return;
        }
        
        // Check for single-flight request - if same request is active, queue callback
        ActiveRequest existingRequest = activeRequests.get(requestKey);
        if (existingRequest != null && !existingRequest.call.isCanceled()) {
            Log.d(TAG, "Request already in flight, queuing for result: " + requestKey);
            synchronized (pendingRequests) {
                pendingRequests.add(new QueuedCallback(query, requestKey, callback, System.currentTimeMillis()));
            }
            return;
        }
        
        // Check global throttling - minimum interval between requests
        if (currentTime - lastSuccessfulRequestTime < MIN_REQUEST_INTERVAL_MS) {
            long delay = MIN_REQUEST_INTERVAL_MS - (currentTime - lastSuccessfulRequestTime);
            Log.d(TAG, "Global throttling active, delaying request by " + delay + "ms");
            scheduleRetry(query, requestKey, callback, delay);
            return;
        }
        
        // Check if another request is globally in progress
        if (isGlobalRequestInProgress.compareAndSet(false, true)) {
            Log.d(TAG, "Starting new request (global lock acquired): " + requestKey);
            executeInternal(query, requestKey, callback);
        } else {
            Log.d(TAG, "Global request in progress, queuing for execution: " + requestKey);
            synchronized (pendingRequests) {
                pendingRequests.add(new QueuedCallback(query, requestKey, callback, System.currentTimeMillis()));
            }
        }
    }
    
    /**
     * Internal execution with proper error handling and retry logic
     */
    private void executeInternal(String query, String requestKey, RequestCallback callback) {
        Call<OverpassResponse> call = apiService.getPoisByQuery(query);
        ActiveRequest activeRequest = new ActiveRequest(call, System.currentTimeMillis());
        activeRequests.put(requestKey, activeRequest);
        
        Log.d(TAG, "Executing API call: " + requestKey);
        
        call.enqueue(new Callback<OverpassResponse>() {
            @Override
            public void onResponse(Call<OverpassResponse> call, Response<OverpassResponse> response) {
                activeRequests.remove(requestKey);
                isGlobalRequestInProgress.set(false);
                
                Log.d(TAG, "=== API RESPONSE RECEIVED ===");
                Log.d(TAG, "RequestKey: " + requestKey);
                Log.d(TAG, "Response code: " + response.code());
                
                if (response.isSuccessful() && response.body() != null) {
                    // Success - update timing and notify all queued callbacks
                    lastSuccessfulRequestTime = System.currentTimeMillis();
                    rateLimitBackoffUntil = 0; // Clear any rate limit backoff
                    
                    Log.d(TAG, "Request successful: " + response.body().getElements().size() + " elements");
                    
                    // Notify original callback
                    callback.onSuccess(response.body());
                    
                    // Notify all queued callbacks for this request AND trigger next in queue
                    notifyQueuedCallbacks(requestKey, response.body(), null);
                    
                } else if (response.code() == 429) {
                    // Rate limit detected - apply exponential backoff
                    handleRateLimit(query, requestKey, callback, response.code());
                    // Release and try next request in queue
                    triggerNextQueuedRequest();
                    
                } else {
                    // Other HTTP error
                    String errorMsg = "HTTP " + response.code();
                    if (response.errorBody() != null) {
                        try {
                            errorMsg += ": " + response.errorBody().string();
                        } catch (Exception e) {
                            errorMsg += " (error body unreadable)";
                        }
                    }
                    
                    Log.e(TAG, "HTTP error: " + errorMsg);
                    
                    if (response.code() >= 500 && response.code() < 600) {
                        handleServerError(query, requestKey, callback, response.code());
                        triggerNextQueuedRequest();
                    } else {
                        // Client errors - don't retry
                        callback.onError(errorMsg);
                        notifyQueuedCallbacks(requestKey, null, errorMsg);
                    }
                }
            }
            
            @Override
            public void onFailure(Call<OverpassResponse> call, Throwable t) {
                activeRequests.remove(requestKey);
                isGlobalRequestInProgress.set(false);
                
                Log.e(TAG, "=== API FAILURE ===");
                Log.e(TAG, "RequestKey: " + requestKey);
                Log.e(TAG, "Failure: " + t.getMessage());
                
                if (call.isCanceled()) {
                    Log.d(TAG, "Call was canceled");
                    notifyQueuedCallbacks(requestKey, null, "Request canceled");
                    return;
                }
                
                // Network failure - apply retry with exponential backoff
                handleNetworkFailure(query, requestKey, callback, t);
                // Try next request in queue while this one waits for retry
                triggerNextQueuedRequest();
            }
        });
    }
    
    /**
     * Handle HTTP 429 rate limiting with exponential backoff
     */
    private void handleRateLimit(String query, String requestKey, RequestCallback callback, int statusCode) {
        Log.w(TAG, "=== RATE LIMIT DETECTED (HTTP 429) ===");
        Log.w(TAG, "RequestKey: " + requestKey);
        
        // Calculate exponential backoff
        long backoffMs = calculateBackoffMs();
        rateLimitBackoffUntil = System.currentTimeMillis() + backoffMs;
        
        Log.w(TAG, "Applying rate limit backoff: " + backoffMs + "ms");
        Log.w(TAG, "Backoff until: " + rateLimitBackoffUntil);
        
        // Schedule retry with backoff
        scheduleRetry(query, requestKey, callback, backoffMs);
    }
    
    /**
     * Handle server errors (5xx) with retry
     */
    private void handleServerError(String query, String requestKey, RequestCallback callback, int statusCode) {
        Log.w(TAG, "=== SERVER ERROR (HTTP " + statusCode + ") ===");
        Log.w(TAG, "RequestKey: " + requestKey);
        
        // Apply retry delay for server errors
        long retryDelay = RETRY_DELAY_BASE_MS * 2; // 4 seconds for server errors
        scheduleRetry(query, requestKey, callback, retryDelay);
    }
    
    /**
     * Handle network failures with retry
     */
    private void handleNetworkFailure(String query, String requestKey, RequestCallback callback, Throwable t) {
        Log.w(TAG, "=== NETWORK FAILURE ===");
        Log.w(TAG, "RequestKey: " + requestKey);
        Log.w(TAG, "Failure: " + t.getMessage());
        
        // Apply retry delay for network failures
        long retryDelay = RETRY_DELAY_BASE_MS; // 2 seconds for network failures
        scheduleRetry(query, requestKey, callback, retryDelay);
    }
    
    /**
     * Calculate exponential backoff for rate limiting
     */
    private long calculateBackoffMs() {
        // Simple exponential backoff: 5s -> 10s -> 20s -> 40s -> 60s (max)
        long baseBackoff = RATE_LIMIT_BACKOFF_INITIAL_MS;
        long currentBackoff = baseBackoff;
        
        // Calculate how many times we've been rate limited recently
        long timeSinceLastSuccess = System.currentTimeMillis() - lastSuccessfulRequestTime;
        if (timeSinceLastSuccess < 60000) { // Within last minute
            // We've been rate limited recently, apply exponential backoff
            int multiplier = (int) (60000 / timeSinceLastSuccess); // Rough estimate
            currentBackoff = Math.min(baseBackoff * (1L << Math.min(multiplier, 4)), RATE_LIMIT_BACKOFF_MAX_MS);
        }
        
        return currentBackoff;
    }
    
    /**
     * Schedule retry with delay
     */
    private void scheduleRetry(String query, String requestKey, RequestCallback callback, long delayMs) {
        mainHandler.postDelayed(() -> {
            Log.d(TAG, "Retrying request after delay: " + requestKey);
            executeRequest(query, requestKey, callback);
        }, delayMs);
    }
    
    /**
     * Notify all queued callbacks for a request and trigger next request in queue
     */
    private void notifyQueuedCallbacks(String requestKey, OverpassResponse response, String error) {
        List<QueuedCallback> toNotify = new ArrayList<>();
        
        synchronized (pendingRequests) {
            Iterator<QueuedCallback> it = pendingRequests.iterator();
            while (it.hasNext()) {
                QueuedCallback qc = it.next();
                if (qc.requestKey.equals(requestKey)) {
                    toNotify.add(qc);
                    it.remove();
                }
            }
        }
        
        // Notify matching callbacks
        for (QueuedCallback qc : toNotify) {
            if (response != null) {
                qc.onSuccess(response);
            } else if (error != null) {
                qc.onError(error);
            }
        }
        
        // IMPORTANT: Trigger next request in queue now that global lock is free
        triggerNextQueuedRequest();
    }

    /**
     * Trigger the next waiting request in the queue
     */
    private void triggerNextQueuedRequest() {
        if (isGlobalRequestInProgress.get()) {
            return;
        }

        QueuedCallback next = null;
        synchronized (pendingRequests) {
            if (!pendingRequests.isEmpty()) {
                next = pendingRequests.remove(0);
            }
        }

        if (next != null) {
            Log.d(TAG, "Processing next request from queue: " + next.requestKey);
            executeRequest(next.query, next.requestKey, next.originalCallback);
        }
    }
    
    /**
     * Cancel all active requests (useful for app lifecycle)
     */
    public void cancelAllRequests() {
        Log.d(TAG, "=== CANCELING ALL REQUESTS ===");
        
        for (ActiveRequest activeRequest : activeRequests.values()) {
            if (!activeRequest.call.isCanceled()) {
                activeRequest.call.cancel();
            }
        }
        
        activeRequests.clear();
        synchronized (pendingRequests) {
            pendingRequests.clear();
        }
        isGlobalRequestInProgress.set(false);
    }
    
    /**
     * Get current request statistics
     */
    public RequestStats getStats() {
        int pendingSize;
        synchronized (pendingRequests) {
            pendingSize = pendingRequests.size();
        }
        return new RequestStats(
            activeRequests.size(),
            pendingSize,
            isGlobalRequestInProgress.get(),
            rateLimitBackoffUntil,
            lastSuccessfulRequestTime
        );
    }
    
    /**
     * Reset rate limiting state (useful for testing or recovery)
     */
    public void resetRateLimitState() {
        Log.d(TAG, "=== RESET RATE LIMIT STATE ===");
        rateLimitBackoffUntil = 0;
        lastSuccessfulRequestTime = 0;
        isGlobalRequestInProgress.set(false);
    }
    
    // Callback interface
    public interface RequestCallback {
        void onSuccess(OverpassResponse response);
        void onError(String errorMessage);
    }
    
    // Internal classes
    private static class ActiveRequest {
        final Call<OverpassResponse> call;
        final long startTime;
        
        ActiveRequest(Call<OverpassResponse> call, long startTime) {
            this.call = call;
            this.startTime = startTime;
        }
    }
    
    private static class QueuedCallback implements RequestCallback {
        final String query;
        final String requestKey;
        final RequestCallback originalCallback;
        final long queueTime;
        
        QueuedCallback(String query, String requestKey, RequestCallback originalCallback, long queueTime) {
            this.query = query;
            this.requestKey = requestKey;
            this.originalCallback = originalCallback;
            this.queueTime = queueTime;
        }
        
        @Override
        public void onSuccess(OverpassResponse response) {
            originalCallback.onSuccess(response);
        }
        
        @Override
        public void onError(String errorMessage) {
            originalCallback.onError(errorMessage);
        }
    }
    
    // Statistics class
    public static class RequestStats {
        public final int activeRequests;
        public final int queuedCallbacks;
        public final boolean globalRequestInProgress;
        public final long rateLimitBackoffUntil;
        public final long lastSuccessfulRequestTime;
        
        RequestStats(int activeRequests, int queuedCallbacks, boolean globalRequestInProgress,
                    long rateLimitBackoffUntil, long lastSuccessfulRequestTime) {
            this.activeRequests = activeRequests;
            this.queuedCallbacks = queuedCallbacks;
            this.globalRequestInProgress = globalRequestInProgress;
            this.rateLimitBackoffUntil = rateLimitBackoffUntil;
            this.lastSuccessfulRequestTime = lastSuccessfulRequestTime;
        }
        
        @Override
        public String toString() {
            return "RequestStats{" +
                    "activeRequests=" + activeRequests +
                    ", queuedCallbacks=" + queuedCallbacks +
                    ", globalRequestInProgress=" + globalRequestInProgress +
                    ", rateLimitBackoffUntil=" + rateLimitBackoffUntil +
                    ", lastSuccessfulRequestTime=" + lastSuccessfulRequestTime +
                    '}';
        }
    }
}
