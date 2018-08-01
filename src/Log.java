import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class Log {
	public String logfile;
	public Boolean debug;
	BufferedWriter fileWriter;
	
	public Log(String lf, Boolean d){
		this.logfile = lf;
		this.debug = d;
		System.out.println("=> Logfile is open for writing!");
	}
	
	public void openLog(){
		try {
			fileWriter = new BufferedWriter(new FileWriter(logfile));
		} catch (IOException e) { e.printStackTrace(); }
	}
	
	public void closeLog(){
		try {
			fileWriter.close();
		} catch (IOException e) { e.printStackTrace(); }
	}
	
	public void writeLog(String s){
		try {
			fileWriter.write(s);
			if(debug){ System.out.println(s); }
		} catch (IOException e) { e.printStackTrace(); }
	}
}