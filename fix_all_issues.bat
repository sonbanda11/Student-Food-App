@echo off
echo Fixing all remaining issues...

:: Fix 1: HomeFragment tab names
powershell -Command "$content = Get-Content 'app\src\main\java\com\example\studentfood\presentation\ui\fragment\HomeFragment.java' -Raw; $content = $content -replace [char]34+'Nhà hàng'+[char]34+', '+[char]34+'Chox & Siêu thi'+[char]34+', '+[char]34+'Máy bán nýoc'+[char]34, [char]34+'Nhà hàng'+[char]34+', '+[char]34+'Chox & Siêu thi'+[char]34+', '+[char]34+'Máy bán hàng'+[char]34+', '+[char]34+'Quán nýoc'+[char]34+', '+[char]34+'Ð án nhanh'+[char]34; $content | Set-Content 'app\src\main\java\com\example\studentfood\presentation\ui\fragment\HomeFragment.java'"

:: Fix 2: NearbyFragment tab names  
powershell -Command "$content = Get-Content 'app\src\main\java\com\example\studentfood\presentation\ui\fragment\NearbyFragment.java' -Raw; $content = $content -replace 'tab\.setText([char]34+'Chox & Siêu '+[char]34)', 'tab.setText([char]34+'Chox & Siêu thi'+[char]34)'; $content | Set-Content 'app\src\main\java\com\example\studentfood\presentation\ui\fragment\NearbyFragment.java'"

powershell -Command "$content = Get-Content 'app\src\main\java\com\example\studentfood\presentation\ui\fragment\NearbyFragment.java' -Raw; $content = $content -replace 'tab\.setText([char]34+'Máy nýoc'+[char]34)', 'tab.setText([char]34+'Máy bán hàng'+[char]34)'; $content | Set-Content 'app\src\main\java\com\example\studentfood\presentation\ui\fragment\NearbyFragment.java'"

powershell -Command "$content = Get-Content 'app\src\main\java\com\example\studentfood\presentation\ui\fragment\NearbyFragment.java' -Raw; $content = $content -replace 'tab\.setText([char]34+'Cà phê'+[char]34)', 'tab.setText([char]34+'Quán nýoc'+[char]34)'; $content | Set-Content 'app\src\main\java\com\example\studentfood\presentation\ui\fragment\NearbyFragment.java'"

powershell -Command "$content = Get-Content 'app\src\main\java\com\example\studentfood\presentation\ui\fragment\NearbyFragment.java' -Raw; $content = $content -replace 'tab\.setText([char]34+'Fast food'+[char]34)', 'tab.setText([char]34+'Ð án nhanh'+[char]34)'; $content | Set-Content 'app\src\main\java\com\example\studentfood\presentation\ui\fragment\NearbyFragment.java'"

echo All fixes applied!
pause
