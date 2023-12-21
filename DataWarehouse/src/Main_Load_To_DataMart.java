import controller.Controller;
import dao.ForecastResultsDao;
import database.DBConnection;
import entity.Config;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class Main_Load_To_DataMart {
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
                    //(Load To DataMart)11. Kiểm tra xem status có phải là AGGREGATED hay không
                    if (status.equals("AGGREGATED")) {
                        controller.loadToDataMart(connection, config);
                    }
                    System.out.println("End");
                }
            }
            // 6. Đóng kết nối database
            db.closeConnection();
        } catch (SQLException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
