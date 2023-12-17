import controller.Controller;
import dao.ForecastResultsDao;
import database.DBConnection;
import entity.Config;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class Main {

    public static void main(String[] args) {
        DBConnection db = new DBConnection();
        ForecastResultsDao dao = new ForecastResultsDao();
        //3. Kết nối với với database Controller
        try (Connection connection = db.getConnection()) {
            //4. Lấy danh sách config trong table config có flag = 1
            List<Config> configs = dao.getConfigs(connection);
            Controller controller = new Controller();
            //5. Duyệt for lấy lần lượt từng config trong list
            for (Config config : configs) {
                int maxWait = 0;
                //7. Khi có processing nào chạy và thời gian dưới 3 phút
                while (dao.getProcessingCount(connection) != 0 && maxWait <= 3) {
                    System.out.println("Wait...");
                    // 8. Chờ 1 phút, tăng biến thời gian
                    maxWait++;
                    Thread.sleep(60000); //60s
                }
                //9. Kiểm tra xem còn processing nào đang chạy không
                if (dao.getProcessingCount(connection) == 0) { //Hết process đang chạy
                    System.out.println("Start");
                    //10. Lấy status của config
                    String status = config.getStatus();
                    //Nếu lỗi thì không cần thực hiện
                    if (status.equals("ERROR")) {
                        continue;
                    }
                    //(Extract)11. Kiểm tra xem status có phải là OFF hay FINISHED hay không
                    else if (status.equals("OFF") || status.equals("FINISHED")) {
                        controller.getData(connection, config);
                    }
                    //(Extract To Staging)11. Kiểm tra xem status có phải là CRAWLED hay không
                    else if (status.equals("CRAWLED")) {
                        controller.extractToStaging(connection, config);
                    }
                    //(Transform Data)11. Kiểm tra xem status có phải là EXTRACTED hay không
                    else if (status.equals("EXTRACTED")) {
                        controller.transformData(connection, config);
                    }
                    //(Load To WH)11. Kiểm tra xem status có phải là TRANSFORMED hay không
                    else if (status.equals("TRANSFORMED")) {
                        controller.loadToWH(connection, config);
                    }
                }
            }
            // 6. Đóng kết nối database
            db.closeConnection();
        } catch (SQLException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
