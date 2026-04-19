// Test queries for Overpass API - copy these to test manually

// 1. Simple restaurant query (should work)
"[out:json][timeout:10];node[amenity=restaurant](around:200,21.0285,105.8542);out body;"

// 2. Multiple amenity types
"[out:json][timeout:10];(node[amenity~restaurant|cafe|fast_food](around:200,21.0285,105.8542););out body;"

// 3. All food-related amenities
"[out:json][timeout:10];(node[amenity~restaurant|cafe|fast_food|food_court|bar|pub|ice_cream](around:500,21.0285,105.8542););out body;"

// 4. Include shops
"[out:json][timeout:10];(node[amenity~restaurant|cafe|fast_food][around:500,21.0285,105.8542];node[shop~supermarket|convenience|grocery|bakery][around:500,21.0285,105.8542];);out body;"
