package dao;

import entity.Config;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ForecastResultsDao {

    public static List<Config> getConfigs(Connection connection) {
        List<Config> configs = new ArrayList<>();
        //Câu select lấy list config muốn run
        String query = "SELECT * FROM config WHERE flag = 1 ORDER BY update_at DESC";
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(query)) {

            while (resultSet.next()) {
                int id = resultSet.getInt("id");
                String author = resultSet.getString("author");
                String email = resultSet.getString("email");
                String fileName = resultSet.getString("filename");
                String directory = resultSet.getString("directory_file");
                String status = resultSet.getString("status_config");
                int flag = resultSet.getInt("flag");
                String detailFilePath = resultSet.getString("detail_file_path");
                Timestamp timestamp = resultSet.getTimestamp("update_at");
                configs.add(new Config(id, author, email, fileName, directory, status, flag, timestamp, detailFilePath));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return configs;
    }

    public static void updateStatus(Connection connection, int id, String status) {
        try (CallableStatement callableStatement = connection.prepareCall("{CALL UpdateStatus(?,?)}")) {
            callableStatement.setInt(1, id);
            callableStatement.setString(2, status);
            callableStatement.execute();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static void updateDetailFilePath(Connection connection, int id, String detailFilePath) {
        try (CallableStatement callableStatement = connection.prepareCall("{CALL UpdatePathFileDetail(?,?)}")) {
            callableStatement.setInt(1, id);
            callableStatement.setString(2, detailFilePath);
            callableStatement.execute();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static void setFlagIsZero(Connection connection, int id) {
        try (CallableStatement callableStatement = connection.prepareCall("{CALL SetFlagIsZero(?)}")) {
            callableStatement.setInt(1, id);
            callableStatement.execute();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static void insertLog(Connection connection, int idConfig, String status, String description) {
        try (CallableStatement callableStatement = connection.prepareCall("{Call InsertLog(?,?,?)}")) {
            callableStatement.setInt(1, idConfig);
            callableStatement.setString(2, status);
            callableStatement.setString(3, description);
            callableStatement.execute();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static List<String> getLogs(Connection connection, int idConfig) {
        List<String> logs = new ArrayList<>();
        //Câu select lấy list config muốn run
        String query = "SELECT * FROM log WHERE id_config = ? ORDER BY created_at ASC";
        try {
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setInt(1, idConfig);
            ResultSet resultSet = statement.executeQuery();
            int i = 1;
            while (resultSet.next()) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(i++ + ". ");
                stringBuilder.append("ID Config: " + resultSet.getInt("id_config"));
                stringBuilder.append(". Status: " + resultSet.getString("status"));
                stringBuilder.append(". Description: " + resultSet.getString("description"));
                stringBuilder.append("Time: " + resultSet.getTimestamp("created_at").toString());
                logs.add(stringBuilder.toString());
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return logs;
    }

    public static void updateIsProcessing(Connection connection, int id, boolean isProcessing) {
        try (CallableStatement callableStatement = connection.prepareCall("{CALL UpdateIsProcessing(?,?)}")) {
            callableStatement.setInt(1, id);
            if (isProcessing) {
                callableStatement.setInt(2, 1);
            } else {
                callableStatement.setInt(2, 0);
            }
            callableStatement.execute();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static int getProcessingCount(Connection connection) {
        int count = 0;
        String query = "SELECT * FROM config WHERE is_processing = 1";
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(query)) {
            while (resultSet.next()) {
                count++;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return count;
    }
}
