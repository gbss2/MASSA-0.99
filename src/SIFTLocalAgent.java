import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.io.IOException;

import jade.lang.acl.ACLMessage;
import jade.lang.acl.UnreadableException;
import jade.core.AID;
import jade.core.behaviours.FSMBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.ParallelBehaviour;
import jade.lang.acl.MessageTemplate;

public class SIFTLocalAgent extends DBagent{
	private static final long serialVersionUID = 1L;

	/* Attributes */
	BioDataLite snpdata;
	int input_length;
	int[] input;
	public String sn = "localhost";
	public String md = "massa";
	public String user = "massa";
	public String key = "@sdfghjkl!";
	MySQLcon myconnection;

	/* Constructor */
	public SIFTLocalAgent() {
		this.setDBname("SIFT");
		this.setInformation("SIFT");
	}

	/* Agent setup */
	protected void setup() {
		System.out.println("Agent "+getLocalName()+" started.");
		this.register();
		this.dbConnect(sn, md, user, key);
		addBehaviour(new waitRequest());
	}

	/* Agent shutdown */
	protected void takeDown() {
		System.out.println("Agent" + getLocalName() + " shutdown.");
		mysqlDisconnect(conn);
	}	

	/* BEHAVIOURS */
	
	/* Agent communication: wait for messages */
	class waitRequest extends CyclicBehaviour {
		private static final long serialVersionUID = 1L;
		private MessageTemplate simplerequest_template;
		private AID sender;

		public waitRequest() { MessageTemplate.MatchPerformative(ACLMessage.REQUEST); }

		public void action() {
			ACLMessage msg = myAgent.receive(simplerequest_template);
			if (msg != null) {
				System.out.println("Agent " + getLocalName() +" received a REQUEST message from agent "+msg.getSender().getName());
				try {
					sender = msg.getSender();
					snpdata = (BioDataLite)msg.getContentObject();
					addBehaviour(new SIFTAction(snpdata.getSnpIdList(),sender,snpdata.getSearchid()));
				} catch (UnreadableException e) { e.printStackTrace(); }
			} else { block(); }
		}
	}

	/* Agent action: access DB -> accessDBsnpLocal; send reply  */
	class SIFTAction extends FSMBehaviour {
		private static final long serialVersionUID = 1L;

		private static final String STATE_A = "A";
		private static final String STATE_B = "B";
		
		public AID partneragent;
		public int[] snpidlist;

		public SIFTAction(int[] sl, AID pa, int asi){
			partneragent = pa;
			snpidlist = sl;
			setAnnsearchid(asi);

			System.out.println("Agent " + getLocalName() +" executing request...");
			this.registerFirstState(new accessSIFT(snpidlist), STATE_A);
			this.registerLastState(new SendReply(partneragent), STATE_B);
			this.registerDefaultTransition(STATE_A,STATE_B);
		}

		public int onEnd() {
			System.out.println("Agent " + getLocalName() +" finished task.");
			return super.onEnd();
		}
	}

	/* Agent reply: send message to Coordinator on task end */
	class SendReply extends OneShotBehaviour {
		private static final long serialVersionUID = 1L;
		private AID     msgreceiver;
		private String  msgperformative;
		private BioDataLite msgcontent;
		private ACLMessage msg;

		public SendReply(AID p) {
			msgreceiver     = p;
			msgcontent      = new BioDataLite();
			msgperformative = "INFORM";
			msgcontent.setSearchid(getAnnsearchid());
		}

		public void action() {
			try {
				System.out.println("... sending "+msgperformative+" to agent: \""+msgreceiver.getName());
				msg = new ACLMessage(ACLMessage.INFORM);
				msg.addReceiver(msgreceiver);
				msg.setLanguage("English");
				msg.setContentObject(msgcontent);
				myAgent.send(msg);
			} catch (IOException e) { e.printStackTrace(); }
		}
	}
	
	/* Agent action: parallel access to DB -> remoteAccessDBsnpLocal  */	
	class accessSIFT extends ParallelBehaviour {
		private static final long serialVersionUID = 1L;

		private int[] snpidlist;

		public accessSIFT (int[] sl) {
			super(WHEN_ALL);
			this.snpidlist = sl;
		}

		public void onStart(){
			for (int i = 0; i < snpidlist.length; i++) {
				if(snpidlist[i]>0){ this.addSubBehaviour(new mysqlLocalSIFTquery(snpidlist[i])); }
			}
		}

		public int onEnd(){
			System.out.println("Agent " + getLocalName() +" finished.");
			return super.onEnd();
		}
	}

	/* Agent action: Query DB  */
	class mysqlLocalSIFTquery extends OneShotBehaviour {
		private static final long serialVersionUID = 1L;

		private int snpid;

		public mysqlLocalSIFTquery(int l) { snpid = l; }
		public void action() { QuerySIFT(snpid); }
	}


	/* LIST OF QUERIES */
	
	/* Agent action: Query SIFT  */
	public void QuerySIFT(int s) {
		String snpid = "rs"+Integer.toString(s);
		
		String refSeqID = "";
		String subst = "";
		String predict = "";
		String score = "";
		String median = "";
		
		String strsql = "SELECT snp132.RefSeqId, " +
						"snp132.Substitution, " +
						"snp132.Prediction, " +
						"snp132.Score, " +
						"snp132.Median " +
						"FROM snp132 " +
						"WHERE " +
						"snp132.rsid = ? ;";
		
		try {
			PreparedStatement pstmt = conn.prepareStatement(strsql);
			pstmt.setString(1,snpid);
			ResultSet rs = pstmt.executeQuery();	
		    
			while (rs.next()) {
				if(refSeqID == ""){ refSeqID = rs.getString(1); }
				else { refSeqID=refSeqID+","+rs.getString(1); }
	
				if(subst == ""){ subst = rs.getString(2); }
				else { subst=subst+","+rs.getString(2); }
	
				if(predict == ""){ predict = rs.getString(3); }
				else { predict=predict+","+rs.getString(3); }
	
				if(score == ""){ score = rs.getString(4); }
				else { score=score+","+rs.getString(4); }
	
				if(median == ""){ median = rs.getString(5); }
				else { median=median+","+rs.getString(5); }
			}
			
			rs.close();
			pstmt.close();
		
			String sql = "insert ignore into sift (rsid,proteinID,subs,pred,score,median,fk_searchid) values ('"+snpid+"','"+refSeqID+"','"+subst+"','"+predict+"','"+score+"','"+median+"',"+this.getAnnsearchid() +")";
			
			Statement stm = annconnection.createStatement();
			stm.executeUpdate(sql);
			stm.close();
		} catch(SQLException e) {
			System.out.println("SQL Exception... Error in querying:");
			e.printStackTrace();
		}
	}
}