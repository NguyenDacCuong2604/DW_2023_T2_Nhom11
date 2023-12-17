-- Tạo database staging 
drop database if exists staging;
create database staging character set utf8;
use staging;


-- Tạo table staging để chứa dữ liệu từ file csv load vào
DROP table if exists staging;
create table staging (
    id INT PRIMARY KEY AUTO_INCREMENT,
    cod VARCHAR(10) NULL DEFAULT NULL,
    message VARCHAR(100) NULL DEFAULT NULL,
    cnt VARCHAR(100) NULL DEFAULT NULL,
    city_id VARCHAR(100) NULL DEFAULT NULL,
    city_name VARCHAR(100) NULL DEFAULT NULL,
    city_latitude VARCHAR(100) NULL DEFAULT NULL,
    city_longitude VARCHAR(100) NULL DEFAULT NULL,
    city_country_code VARCHAR(10) NULL DEFAULT NULL,
    city_population VARCHAR(100) NULL DEFAULT NULL,
    city_timezone VARCHAR(100) NULL DEFAULT NULL,
    city_sunrise VARCHAR(100) NULL DEFAULT NULL,
    city_sunset VARCHAR(100) NULL DEFAULT NULL,
    dt VARCHAR(100) NULL DEFAULT NULL,
    dt_txt VARCHAR(100) NULL DEFAULT NULL,
    main_temp VARCHAR(100) NULL DEFAULT NULL,
    main_feels_like VARCHAR(100) NULL DEFAULT NULL,
    main_temp_min VARCHAR(100) NULL DEFAULT NULL,
    main_temp_max VARCHAR(100) NULL DEFAULT NULL,
    main_pressure VARCHAR(100) NULL DEFAULT NULL,
		main_sea_level VARCHAR(100) NULL DEFAULT NULL,
    main_grnd_level VARCHAR(100) NULL DEFAULT NULL,
    main_humidity VARCHAR(100) NULL DEFAULT NULL,
    main_temp_kf VARCHAR(100) NULL DEFAULT NULL,
    weather_id VARCHAR(100) NULL DEFAULT NULL,
    weather_main VARCHAR(50) NULL DEFAULT NULL,
    weather_description VARCHAR(100) NULL DEFAULT NULL,
    weather_icon VARCHAR(50) NULL DEFAULT NULL,
    clouds_all VARCHAR(100) NULL DEFAULT NULL,
    wind_speed VARCHAR(100) NULL DEFAULT NULL,
    wind_deg VARCHAR(100) NULL DEFAULT NULL,
    wind_gust VARCHAR(100) NULL DEFAULT NULL,
    visibility VARCHAR(100) NULL DEFAULT NULL,
    pop VARCHAR(100) NULL DEFAULT NULL,
    rain_3h VARCHAR(100) NULL DEFAULT NULL,
    sys_pod VARCHAR(5) NULL DEFAULT NULL,
    created_date VARCHAR(100) NULL DEFAULT NULL
);

-- Tạo procedure TransformDate để tham chiếu khóa chính table date_dim của warehouse vào thuộc tính _date của staging
DROP PROCEDURE if EXISTS TransformDate;
CREATE PROCEDURE TransformDate()
BEGIN
		ALTER TABLE staging ADD COLUMN if not exists _date INT;
    UPDATE staging
    JOIN warehouse.date_dim AS dim ON CAST(staging.dt_txt AS DATE) = dim.full_date
    SET staging._date = dim.date_sk;
END;

-- Tạo procedure TransformTime để tham chiếu khóa chính của table time_dim của warehouse vào thuộc tính _time của staging
DROP PROCEDURE IF EXISTS TransformTime;
CREATE PROCEDURE TransformTime()
BEGIN
		ALTER TABLE staging ADD COLUMN if not exists _time INT;
		UPDATE staging
		JOIN warehouse.time_dim AS dim ON CAST(staging.dt_txt AS TIME) = dim.full_time
		SET staging._time = dim.time_sk;
END;

-- Tạo procedure TransformCity để tham chiếu khóa chính của table city_dim của warehouse vào thuộc tính _city của staging
drop procedure if exists TransformCity;
create procedure TransformCity()
begin
		-- Tạo table tạm để chứa dữ liệu City
		create temporary table TempCity(
			city_id VARCHAR(100)  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
			city_name VARCHAR(100)  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
			city_lat FLOAT,
			city_lon FLOAT,
			city_country_code VARCHAR(100)  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
			city_population INT,
			city_timezone INT
		);
		-- Insert dữ liệu riêng biệt của City vào table tạm
		INSERT into TempCity
		select distinct city_id, city_name, CAST(city_latitude as DOUBLE), CAST(city_longitude as DOUBLE), city_country_code, CAST(city_population as INT), CAST(city_timezone as INT) FROM staging;
		-- Thêm dữ liệu city vào nếu dữ liệu city đó chưa có trong table city_dim
		INSERT into warehouse.city_dim(city_id, city_name, city_lat, city_lon, city_country, city_population, city_timezone)
		select city_id, city_name, city_lat, city_lon, city_country_code, city_population, city_timezone from TempCity
		where not exists (
			select 1 from warehouse.city_dim
			where warehouse.city_dim.city_id = TempCity.city_id 
			&& warehouse.city_dim.city_name = TempCity.city_name
			&& warehouse.city_dim.city_lat = TempCity.city_lat 
			&& warehouse.city_dim.city_lon = TempCity.city_lon
			&& warehouse.city_dim.city_country = TempCity.city_country_code 
			&& warehouse.city_dim.city_population = TempCity.city_population
			&& warehouse.city_dim.city_timezone = TempCity.city_timezone
		);
		-- Cập nhật giá trị _city cho từng dữ liệu trong staging = khóa chính của city_dim
		ALTER TABLE staging ADD COLUMN if not exists _city INT;
		update staging join warehouse.city_dim as dim on 
		staging.city_id = dim.city_id &&
		staging.city_name = dim.city_name &&
		cast(staging.city_latitude as FLOAT) = dim.city_lat &&
		cast(staging.city_longitude as FLOAT) = dim.city_lon &&
		staging.city_country_code = dim.city_country &&
		cast(staging.city_population as INT) = dim.city_population &&
		cast(staging.city_timezone as INT) = dim.city_timezone
		set staging._city = dim.id;
		-- Xóa table tạm
		drop TEMPORARY table TempCity;
end;

-- Tạo procedure TransformWeather để tham chiếu khóa chính của table weather_dim của warehouse vào thuộc tính _weather của staging
drop procedure if exists TransformWeather;
create procedure TransformWeather()
begin
		-- Tạo table tạm để chứa dữ liệu Weather
		create temporary table TempWeather(
			weather_id INT,
			weather_main VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
			weather_description VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
			weather_icon VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci
		);
		-- Insert dữ liệu riêng biệt của Weather vào table tạm
		INSERT into TempWeather
		select distinct cast(weather_id as int), weather_main, weather_description, weather_icon FROM staging;
		-- Thêm dữ liệu weather vào nếu dữ liệu weather đó chưa có trong table weather_dim
		INSERT into warehouse.weather_dim(weather_id, weather_main, weather_description, weather_icon)
		select weather_id, weather_main, weather_description, weather_icon from TempWeather
		where not exists (
			select 1 from warehouse.weather_dim as dim
			where dim.weather_id = TempWeather.weather_id &&
						dim.weather_main = TempWeather.weather_main &&
						dim.weather_description = TempWeather.weather_description &&
						dim.weather_icon = TempWeather.weather_icon
						
		);
		-- Cập nhật giá trị _weather cho từng dữ liệu trong staging = khóa chính của weather_dim
		ALTER TABLE staging ADD COLUMN if not exists _weather INT;
		update staging join warehouse.weather_dim as dim on 
			cast(staging.weather_id as INT) = dim.weather_id &&
			staging.weather_main = dim.weather_main &&
			staging.weather_description = dim.weather_description &&
			staging.weather_icon = dim.weather_icon
		set staging._weather = dim.id;
		-- Xóa table tạm		
		drop TEMPORARY table TempWeather;
end;




	



