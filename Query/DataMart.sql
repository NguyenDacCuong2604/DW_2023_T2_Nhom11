-- Tạo database DataMart
drop database if exists DataMart;
create database DataMart;
use DataMart;

-- Tạo table forecast để chứa dữ liệu mà report connect vào để lấy dữ liệu
drop table if exists forecast;
create table forecast(
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

-- Tạo table forecast_temp để chứa dữ liệu tạm khi lấy từ warehouse
drop table if exists forecast_temp;
create table forecast_temp(
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