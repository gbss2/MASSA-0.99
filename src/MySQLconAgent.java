import jade.core.behaviours.OneShotBehaviour;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class MySQLconAgent extends DBagent {
	private static final long serialVersionUID = 1L;

	String driverName = "com.mysql.jdbc.Driver";
	String serverName = "150.164.28.6:3306";
	String mydatabase ="giordano";
	String url = "jdbc:mysql://" + serverName + "/" + mydatabase;
	String user = "giordano1";
	String key = "giordano";
	ResultSet rs;
	public static String sql = "Select * from example_autoincrement";
	public static Statement stm;
	public static Connection conn = null;
	
    public MySQLconAgent() {
    	this.setDBname("mysql");
   	 	this.setInformation("mysql"); 
    }

    protected void setup() {
        System.out.println("Agent "+getLocalName()+" started.");
        this.register();

        addBehaviour(new OneShotBehaviour(this){
			private static final long serialVersionUID = 1L;

			public void action() {
				try{ Class.forName("com.mysql.jdbc.Driver").newInstance(); }
				catch (Exception e){ System.out.println("Class strife. " + e); }

				try {
					conn = DriverManager.getConnection(url, user, key);
					System.out.println("Successful Connection ");
				} catch (SQLException e) {
				    System.out.println("SQLException: " + e.getMessage());
				    System.out.println("SQLState: " + e.getSQLState());
				    System.out.println("VendorError: " + e.getErrorCode());
				}
				 
				QueryOne();
				try {
				    conn.close();
				    System.out.println("Disconnected");
				} catch(SQLException erro) {
				    System.out.println("Error while Disconnecting");
				    erro.printStackTrace();
				}
			}
		});
    }

	public static void QueryOne() {
		try {
			Statement stm = (Statement) conn.createStatement();
			ResultSet rs = stm.executeQuery(sql);	
			System.out.println("Successful Query");

			while (rs.next()) {
				Integer id_gp = rs.getInt("id");
				String nome_gp = rs.getString("data");                  
				System.out.println("ID: " + id_gp + " Data: " +nome_gp);
			}
		} catch(SQLException e) {
			System.out.println("SQL Exception... Error in querying:");
			e.printStackTrace();
		}
	}
}