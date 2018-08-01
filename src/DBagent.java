import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.commons.dbcp2.BasicDataSource;

import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;

public class DBagent extends Agent {
	private static final long serialVersionUID = 1L;

	private String dbname;
	
	private List<String> information = new ArrayList<String>();
	private static String servicetype;

	public static String slash;
	public static String os;

	public String annsn = "localhost";
	public String anndbname = "massa";
	public String anndbUser = "massa";
	public String anndbKey = "@sdfghjkl!";

	public static Connection annconnection;
	
	static String driverName = "com.mysql.jdbc.Driver";
	private String serverName; 
	private String mydatabase; 
	private String user;
	private String key; 
	public  ResultSet rs;
	public String sql = null;
	public Statement stm;
	public Connection conn = null;
	public int q; 
	
	private int annsearchid;
	public String[] polTable;
	
    public DBagent() {
    	DBagent.servicetype = "database_search";
        polTable = new String[21];
        this.annConnect();
    }

    protected void setup() {
        System.out.println("Agent "+getLocalName()+" started.");
    }
    
    public static class PooledMysqlConn{
    	public static final BasicDataSource dbSource = new BasicDataSource();	

        public PooledMysqlConn() {
    		dbSource.setDriverClassName(driverName);
    		dbSource.setUrl("jdbc:mysql://localhost:3306/annotation");
    		dbSource.setUsername("massa");
            dbSource.setPassword("@sdfghjkl!");
            dbSource.setPoolPreparedStatements(true);
            dbSource.setMaxOpenPreparedStatements(100);
        }

        public static Connection getConnection() throws SQLException {
            return dbSource.getConnection();
        }
    }

	protected void takeDown() {
		System.out.println("Agent" + getLocalName() + " shutdown.");
	}
    
    protected void setDBname(String n){
    	this.dbname =  n;
    }

    protected String getDBname(){
    	return this.dbname;
    }    

    protected String getServicetype(){
    	return DBagent.servicetype;
    }    
   
    protected void setInformation(String s){
    	this.information.add(s);
    }

    protected List<String> getInformation(){
    	return this.information;
    }
    
    public void annConnect(){
    	MySQLcon(annsn, anndbname, anndbUser, anndbKey);
    	annconnection = mysqlConnect();
    }

    public void annDisconnect(){
    	mysqlDisconnect(annconnection);
    }
    
    public void dbConnect(String sname, String database, String useR, String passKey){
    	MySQLcon(sname, database, useR, passKey);
    	conn = mysqlConnect();
    }

    public void dbDisconnect(){
    	mysqlDisconnect(conn);
    }
    
    public void setAnnConnection(Connection c){
    	annconnection = c;
    }

    protected void register(){
    	String info;
    	try {
    		DFAgentDescription dfd = new DFAgentDescription();
    		dfd.setName(getAID());
    		
			System.out.println("Agent "+getLocalName()+" registering service type \""+this.getServicetype()+"\" with DF");
			
    		Iterator<String> it = this.information.iterator();
    		while (it.hasNext()) {
    			info = it.next();
    			
    			System.out.println("Agent "+getLocalName()+" registering information \""+info+"\" with DF");
    			ServiceDescription sd = new ServiceDescription();
    			sd.setName(info);
    			sd.setType(this.getServicetype());   		

    			dfd.addServices(sd);
    		}	
    		DFService.register(this, dfd);
    	} catch (FIPAException fe) { fe.printStackTrace(); }
    }

	public int getAnnsearchid() {
		return annsearchid;
	}

    public String getServerName(){
    	return serverName;
    }

    public String getMyDatabase(){
    	return mydatabase;
    }

    public String getUser(){
    	return user;
    }

    public String getKey(){
    	return key;
    }
	
	public void setAnnsearchid(int annsearchid) {
		this.annsearchid = annsearchid;
	}
    
    private void MySQLcon(String sname, String database, String useR, String passKey) {
    	serverName = sname;
    	mydatabase = database;
    	user = useR;
    	key = passKey;
    }
    
    public Connection mysqlConnect(){		
    	try{Class.forName("com.mysql.jdbc.Driver").newInstance();}  
    	catch(Exception e) {System.out.println("Class strife. " + e);}  
		 
		try {
			conn = DriverManager.getConnection("jdbc:mysql://"+getServerName()+"/"+getMyDatabase(),getUser(),getKey());
		    System.out.println("Successful Connection ");
		    return conn;
		} catch (SQLException e) {
		    System.out.println("SQLException: " + e.getMessage());
		    System.out.println("SQLState: " + e.getSQLState());
		    System.out.println("VendorError: " + e.getErrorCode());
			    
		    return null;
		}
    }			 

    public void mysqlDisconnect(Connection c){					 
    	try {
    		c.close();
	        System.out.println("Disconnected");
    	} catch(SQLException e) {
    		System.out.println("Error while Disconnecting");
	        e.printStackTrace();
    	}
    }     

    public void clearPolTable(){
    	if(polTable != null){
	    	for (int x = 0; x < polTable.length; x++) {
				polTable[x] = null;
			}
    	}
    }
}