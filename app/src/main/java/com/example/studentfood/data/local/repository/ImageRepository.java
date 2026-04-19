package com.example.studentfood.data.repository;

import android.database.sqlite.SQLiteDatabase;
import com.example.studentfood.data.local.dao.ImageDAO;
import com.example.studentfood.domain.model.Image;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Repository quản lý toàn bộ logic về hình ảnh trong hệ thống.
 * Kết nối giữa ViewModel và ImageDAO.
 */
public class ImageRepository {

    private final ImageDAO imageDAO;
    // Sử dụng Executor để xử lý các tác vụ ghi DB ở luồng phụ (tránh treo UI)
    private final ExecutorService executorService;

    public ImageRepository(SQLiteDatabase db) {
        this.imageDAO = new ImageDAO(db);
        this.executorService = Executors.newSingleThreadExecutor();
    }

    // ================= READ (Đọc dữ liệu) =================

    /**
     * Lấy danh sách ảnh cho một đối tượng bất kỳ (Quán ăn, món ăn, danh mục...)
     */
    public List<Image> getImagesByRef(String refId, Image.RefType refType) {
        return imageDAO.getImagesByRef(refId, refType);
    }

    /**
     * Lấy duy nhất ảnh đại diện cho đối tượng.
     */
    public Image getAvatarImage(String refId, Image.RefType refType) {
        List<Image> images = getImagesByRef(refId, refType);
        for (Image img : images) {
            if (img.isAvatar()) return img;
        }
        return !images.isEmpty() ? images.get(0) : null;
    }

    // ================= WRITE (Ghi dữ liệu - Chạy luồng phụ) =================

    /**
     * Thêm mới một hình ảnh.
     */
    public void addImage(Image img) {
        executorService.execute(() -> imageDAO.insertImage(img));
    }

    /**
     * Lưu danh sách ảnh (thường dùng khi import dữ liệu hoặc cập nhật quán ăn).
     */
    public void addImages(List<Image> images) {
        executorService.execute(() -> {
            for (Image img : images) {
                imageDAO.insertImage(img);
            }
        });
    }

    /**
     * Xóa toàn bộ ảnh của một đối tượng cụ thể.
     */
    public void clearImagesByRef(String refId, Image.RefType refType) {
        executorService.execute(() -> imageDAO.deleteImagesByRef(refId, refType));
    }

    // ================= CLEANUP =================

    public void shutdown() {
        executorService.shutdown();
    }
}