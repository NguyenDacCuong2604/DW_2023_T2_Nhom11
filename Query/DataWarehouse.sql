-- Tạo database warehouse 
DROP database if exists warehouse;
create database warehouse character set utf8;
use warehouse;

-- Tạo table date_dim chứa thông tin về ngày 
DROP TABLE if exists date_dim;
CREATE TABLE date_dim (
    date_sk INT PRIMARY KEY,
    full_date VARCHAR(500),
    day_since_2020 VARCHAR(500),
    month_since_2020 VARCHAR(500),
    day_of_week VARCHAR(500),
    calendar_month VARCHAR(500),
    calendar_year VARCHAR(500),
    calendar_year_month VARCHAR(500),
    day_of_month VARCHAR(500),
    day_of_year VARCHAR(500),
    week_of_year_sunday VARCHAR(500),
    year_week_sunday VARCHAR(500),
    week_sunday_start VARCHAR(500),
    week_of_year_monday VARCHAR(500),
    year_week_monday VARCHAR(500),
    week_monday_start VARCHAR(500),
    holiday VARCHAR(500),
    day_type VARCHAR(500)
);
create index idx_full_date on date_dim(full_date); -- Tạo index để truy vấn vào table date_dim hiệu quả hơn

-- Load dữ liệu date vào table date_dim từ file csv
TRUNCATE TABLE date_dim;
LOAD DATA INFILE 'D:\\Github\\Weather_DataWarehouse\\DataWareHouse\\date_dim_without_quarter.csv' INTO TABLE date_dim FIELDS TERMINATED BY ','
ENCLOSED BY '"'
LINES TERMINATED BY '\n'
( 
date_sk , 
full_date , 
day_since_2020 , 
month_since_2020 , 
day_of_week , 
calendar_month , 
calendar_year , 
calendar_year_month , 
day_of_month , 
day_of_year , 
week_of_year_sunday , 
year_week_sunday , 
week_sunday_start , 
week_of_year_monday , 
year_week_monday , 
week_monday_start , 
holiday , 
day_type 
); 

-- Tạo table time_dim chứa thông tin về giờ trong ngày
DROP TABLE if exists time_dim;
CREATE TABLE time_dim (
    time_sk INT PRIMARY KEY,
    _hour VARCHAR(2),
		_minute VARCHAR(2),
		_second VARCHAR(2),
		full_time Time
);
create index idx_full_time on time_dim(full_time); -- Tạo index để truy vấn vào dữ liệu time_dim hiệu quả hơn

-- Load dữ liệu time vào table time_dim từ file csv
TRUNCATE TABLE time_dim;
LOAD DATA INFILE 'D:\\Github\\Weather_DataWarehouse\\DataWareHouse\\time_dim.csv' INTO TABLE time_dim FIELDS TERMINATED BY ','
-- OPTIONALLY ENCLOSED BY '"' 
ENCLOSED BY '"'
LINES TERMINATED BY '\n'
( 
time_sk , 
_hour,
_minute,
_second,
full_time
); 

-- Tạo table city_dim chứa thông tin về thành phố 
drop table if EXISTS city_dim;
create table city_dim(
	id int primary key auto_increment,
	city_id VARCHAR(100) NULL DEFAULT NULL,
	city_name VARCHAR(100) NULL DEFAULT NULL,
	city_lat FLOAT NULL default 0,
	city_lon FLOAT null DEFAULT 0,
	city_country VARCHAR(100) NULL DEFAULT NULL,
	city_population INT NULL DEFAULT 0,
	city_timezone INT NULL DEFAULT 0,
	dt_changed datetime NULL DEFAULT CURRENT_TIMESTAMP,
  dt_expired datetime NULL DEFAULT NULL
);

-- Tạo table weather_dim chứa thông tin về thời tiết
drop table if exists weather_dim;
create table weather_dim(
	id int primary key auto_increment,
	weather_id INT null default 0,
	weather_main VARCHAR(100) NULL DEFAULT NULL,
	weather_description VARCHAR(100) NULL DEFAULT NULL,
	weather_icon VARCHAR(100) NULL DEFAULT NULL,
	dt_changed datetime NULL DEFAULT current_timestamp,
  dt_expired datetime NULL DEFAULT NULL
);

-- Tạo table fact chứa các id của các bảng dim và các giá trị khác
drop table if exists fact;
CREATE TABLE fact (
    id_fact INT PRIMARY KEY AUTO_INCREMENT,
    id_city INT,
    id_time INT,
		id_date INT,
    id_weather INT,
		city_sunset INT NULL DEFAULT 0,
		city_sunrise INT NULL DEFAULT 0,
    isDelete bit(1) NULL DEFAULT b'0',
		main_temp DOUBLE null default 0,
		main_feels_like DOUBLE null default 0,
		main_temp_min DOUBLE null default 0,
		main_temp_max DOUBLE null default 0,
		main_pressure INT null default 0,
		main_grnd_level INT null DEFAULT 0,
		main_humidity INT null default 0,
		main_temp_kf DOUBLE null default 0,
		clouds_all INT null default 0,
		wind_speed DOUBLE null default 0,
		wind_deg INT null default 0,
		wind_gust DOUBLE null default 0,
		visibility INT null default 0,
		pop DOUBLE null default 0,
		rain_3h DOUBLE null DEFAULT 0,
		sys VARCHAR(1) NULL DEFAULT NULL,
		dtChanged Datetime NULL DEFAULT CURRENT_TIME,
		dtExpired datetime NULL DEFAULT NULL,
    FOREIGN KEY (id_city) REFERENCES city_dim(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    FOREIGN KEY (id_time) REFERENCES time_dim(time_sk) ON DELETE RESTRICT ON UPDATE RESTRICT,
		FOREIGN KEY (id_date) REFERENCES date_dim(date_sk) ON DELETE RESTRICT ON UPDATE RESTRICT,
    FOREIGN KEY (id_weather) REFERENCES weather_dim(id) ON DELETE RESTRICT ON UPDATE RESTRICT
);

-- Tạo trigger dt_expired_city để cập nhật hạn sử dụng của dữ liệu city sau khi insert mỗi dòng dữ liệu
drop trigger if exists dt_expired_city;
create trigger dt_expired_city before insert on city_dim for each row begin set NEW.dt_expired = "9999-12-31 23:59:59";
end;

-- Tạo trigger dt_expried_weather để cập nhật hạn sử dụng của dữ liệu weather sau khi insert mỗi dòng dữ liệu
drop trigger if exists dt_expired_weather;
create trigger dt_expired_weather before insert on weather_dim for each row begin set NEW.dt_expired = "9999-12-31 23:59:59";
end;

-- Tạo trigger dt_expried_fact để cập nhật hạn sử dụng của dữ liệu trong fact sau khi insert mỗi dòng dữ liệu
drop trigger if exists dt_expired_fact;
create trigger dt_expired_fact before insert on fact for each row begin set NEW.dtExpired = "9999-12-31 23:59:59";
end;

-- Tạo procedure SetDataExpired để cập nhật hạn sử dụng của dữ liệu = thời gian hiện tại với điều kiện các dữ liệu có isDelete=0
drop procedure if exists SetDataExpired;
create procedure SetDataExpired()
BEGIN
	UPDATE fact
    SET dtExpired = CURRENT_TIMESTAMP
    WHERE isDelete = b'0';
end;


-- Tạo bảng table aggregate forecast_results để tổng hợp dữ liệu dể chuyển đến data mart
drop table if EXISTS forecast_results;
create table forecast_results(
	id int primary key not null,
	date_of_week varchar(20) null default null,
	date_forecast date null default null,
	time_forecast time null default null,
	city_name VARCHAR(100) null default null,
	main_temp double  null default 0,
	main_pressure int null default 0,
	main_humidity int null default 0,
	clouds_all int null default 0,
	wind_speed double null default 0,
	visibility int null default 0,
	rain_3h int null default 0,
	weather_description varchar(100) null default null,
	weather_icon varchar(100) null default null
);


