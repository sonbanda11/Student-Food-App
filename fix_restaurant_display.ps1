# Fix restaurant display issue in HomeFragment

Write-Host "Fixing restaurant display issue..."

$content = Get-Content "app\src\main\java\com\example\studentfood\presentation\ui\fragment\HomeFragment.java" -Raw

# Remove the problematic init() call and only keep updateData()
$pattern = '// Kh.*i.*t.*o.*adapter.*n.*u.*c.*n.*\s*nearbyRestaurantComponent\.init\(restaurants, HomeRestaurantAdapter\.TYPE_HORIZONTAL\);.*\s*// Update nearby component.*v.*i.*data.*m.*i.*\s*nearbyRestaurantComponent\.updateData\(restaurants\);.*\s*nearbyRestaurantComponent\.setUserLocation\(currentLat, currentLng\);'

$replacement = '// Ch.*i.*update.*data.*n.*u.*c.*n,.*kh.*i.*ng.*init.*l.*i.*\s*if (nearbyRestaurantComponent != null) {\s*nearbyRestaurantComponent\.updateData(restaurants);\s*nearbyRestaurantComponent\.setUserLocation(currentLat, currentLng);\s*}'

# Simple string replacement approach
$content = $content -replace 'nearbyRestaurantComponent\.init\(restaurants, HomeRestaurantAdapter\.TYPE_HORIZONTAL\);', '// nearbyRestaurantComponent.init() - moved to initComponents()'
$content = $content -replace '// Kh.*i.*t.*o.*adapter.*n.*u.*c.*n', '// Component initialized in initComponents()'

$content | Set-Content "app\src\main\java\com\example\studentfood\presentation\ui\fragment\HomeFragment.java"

Write-Host "Restaurant display fix applied!"
