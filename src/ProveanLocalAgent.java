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

public class ProveanLocalAgent extends DBagent{
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
    public ProveanLocalAgent() {
    	this.setDBname("Provean");
   	 	this.setInformation("Provean");
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
					addBehaviour(new ProveanAction(snpdata.getSnpIdList(),sender,snpdata.getSearchid()));
				} catch (UnreadableException e) { e.printStackTrace(); }
			} else { block(); }
		}
	}

	/* Agent action: access DB -> accessDBsnpLocal; send reply  */
	class ProveanAction extends FSMBehaviour {
		private static final long serialVersionUID = 1L;

		private static final String STATE_A = "A";
		private static final String STATE_B = "B";
		
		public AID partneragent;
		public int[] snpidlist;

		public ProveanAction(int[] sl, AID pa, int asi){
			partneragent = pa;
			snpidlist = sl;
			setAnnsearchid(asi);

			System.out.println("Agent " + getLocalName() +" executing request...");
			this.registerFirstState(new accessProvean(snpidlist), STATE_A);
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
	class accessProvean extends ParallelBehaviour {
		private static final long serialVersionUID = 1L;

		private int[] snpidlist;

		public accessProvean (int[] sl) {
			super(WHEN_ALL);
			this.snpidlist = sl;
		}

		public void onStart(){
			for (int i = 0; i < snpidlist.length; i++) {
				if(snpidlist[i]>0){ this.addSubBehaviour(new mysqlLocalProveanquery(snpidlist[i])); }
			}
		}

		public int onEnd(){
			System.out.println("Agent " + getLocalName() +" finished.");
			return super.onEnd();
		}
	}

	/* Agent action: Query DB  */	
	class mysqlLocalProveanquery extends OneShotBehaviour {
		private static final long serialVersionUID = 1L;

		private int snpid;

		public mysqlLocalProveanquery(int l) { snpid = l; }
		public void action() { QueryProvean(snpid); }
	}

	
	/* LIST OF QUERIES */
	
	/* Agent action: Query PROVEAN  */
	public void QueryProvean(int s) {
		String snpid = Integer.toString(s);
		
		String refSeqID = "";
		String subst = "";
		String predict = "";
		String score = "";
		String posProt = "";
		String siftPredict = "";
		String siftScore = "";
		
		String strsql = "SELECT snp137.proteinID, " +
						"snp137.CodonChange, " +
						"snp137.posProt, " +
						"snp137.proveanScore, " +
						"snp137.proveanPredict, " +
						"snp137.siftScore, " +
						"snp137.siftPredict " +
						"FROM snp137 " +
						"WHERE " +
						"snp137.rsid = ? ;";
		
		try {
			PreparedStatement pstmt = conn.prepareStatement(strsql);
			pstmt.setString(1,snpid);
			ResultSet rs = pstmt.executeQuery();	
		    
			while (rs.next()) {
				if(refSeqID == "") { refSeqID = rs.getString(1); }
				else { refSeqID=refSeqID+";"+rs.getString(1); }
	
				if(subst == "") { subst = rs.getString(2); }
				else { subst=subst+";"+rs.getString(2); }
	
				if(posProt == "") { posProt = rs.getString(3); }
				else { posProt=posProt+";"+rs.getString(3); }
	
				if(predict == "") { predict = rs.getString(5); }
				else { predict=predict+";"+rs.getString(5); }
	
				if(score == "") { score = rs.getString(4); }
				else { score=score+";"+rs.getString(4); }
	
				if(siftPredict == "") { siftPredict = rs.getString(7); }
				else { siftPredict=siftPredict+";"+rs.getString(7); }
	
				if(siftScore == "") { siftScore = rs.getString(6); }
				else { siftScore=siftScore+";"+rs.getString(6); }
			}
			
			snpid = "rs"+Integer.toString(s);
			if(!(refSeqID.length()>0)){ refSeqID = "null"; }
			if(!(subst.length()>0)){ subst = "null"; }
			if(!(posProt.length()>0)){ posProt = "null"; }
			if(!(predict.length()>0)){ predict = "null"; }
			if(!(score.length()>0)){ score = "null"; }
			if(!(siftPredict.length()>0)){ siftPredict = "null"; }
			if(!(siftScore.length()>0)){ siftScore = "null"; }
	
			rs.close();
			pstmt.close();
			
			String sql = "insert ignore into provean (rsid,proteinID,subs,median,pred,score,siftScore,siftPredict,fk_searchid) values ('"+snpid+"','"+refSeqID+"','"+subst+"','"+posProt+"','"+predict+"','"+score+"','"+siftScore+"','"+siftPredict+"',"+this.getAnnsearchid() +")";
			
			Statement stm = annconnection.createStatement();
			stm.executeUpdate(sql);
			stm.close();
		} catch(SQLException e) {
			System.out.println("SQL Exception... Error in querying:");
			e.printStackTrace();
		}
	}
}