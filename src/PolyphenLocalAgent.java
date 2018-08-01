import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.io.IOException;

import jade.lang.acl.ACLMessage;
import jade.lang.acl.UnreadableException;
import jade.core.AID;
import jade.core.behaviours.FSMBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.ParallelBehaviour;
import jade.lang.acl.MessageTemplate;

public class PolyphenLocalAgent extends DBagent{
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
    public PolyphenLocalAgent() {
    	this.setDBname("polyphen");
   	 	this.setInformation("polyphen");
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
					addBehaviour(new PolyphenAction(snpdata.getSnpIdList(),sender,snpdata.getSearchid()));
				} catch (UnreadableException e) { e.printStackTrace(); }
			} else { block(); }
		}
	}

	/* Agent action: access DB -> accessDBsnpLocal; send reply  */
	class PolyphenAction extends FSMBehaviour {
		private static final long serialVersionUID = 1L;

		private static final String STATE_A = "A";
		private static final String STATE_B = "B";
		
		public AID partneragent;
		public int[] snpidlist;

		public PolyphenAction(int[] sl, AID pa, int asi){
			partneragent = pa;
			snpidlist = sl;
			setAnnsearchid(asi);

			System.out.println("Agent " + getLocalName() +" executing request...");
			this.registerFirstState(new accessPolyphen(snpidlist), STATE_A);
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
	class accessPolyphen extends ParallelBehaviour {
		private static final long serialVersionUID = 1L;

		private int[] snpidlist;

		public accessPolyphen (int[] sl) {
			super(WHEN_ALL);
			this.snpidlist = sl;
		}

		public void onStart(){
			for (int i = 0; i < snpidlist.length; i++) {
				if(snpidlist[i]>0){ this.addSubBehaviour(new mysqlLocalPolyphenquery(snpidlist[i])); }
			}
		}

		public int onEnd(){
			System.out.println("Agent " + getLocalName() +" finished.");
			return super.onEnd();
		}
	}

	/* Agent action: Query DB  */
	class mysqlLocalPolyphenquery extends OneShotBehaviour {
		private static final long serialVersionUID = 1L;

		private int snpid;

		public mysqlLocalPolyphenquery(int l) { snpid = l; }
		public void action() { QueryPolyphen(snpid); }
	}


	/* LIST OF QUERIES */
	
	/* Agent action: Query POLYPHEN  */
	public void QueryPolyphen(int s) {
		String snpid = "rs"+Integer.toString(s);
		
		String uniProtId = "";
		String subst = "";
		String posProt = "";		
		String predict2 = "";
		String prob2 = "";
		String fdr = "";
		String predict1 = "";
		String sep = "\\";
		
		String strsql = "SELECT humvar.acc, " +
						"CONCAT(nt1,?,nt2) as subs, " +
						"humvar.pos, " +
						"humvar.prediction, " +
						"humvar.pph2_prob, " +
						"humvar.pph2_FDR, " +
						"humvar.effect " +
						"FROM humvar " +
						"WHERE " +
						"humvar.rsid = ? ;";
		
		try {
			PreparedStatement pstmt = conn.prepareStatement(strsql);
			pstmt.setString(1,sep);
			pstmt.setString(2,snpid);
			ResultSet rs = pstmt.executeQuery();	
		    
			while (rs.next()) {
				if(uniProtId == "") { uniProtId = rs.getString(1); }
				else { uniProtId=uniProtId+","+rs.getString(1); }
	
				if(subst == "") { subst = rs.getString(2); }
				else { subst=subst+","+rs.getString(2); }
	
				if(posProt == "") { posProt = rs.getString(3); }
				else { posProt=posProt+","+rs.getString(3); }
	
				if(predict2 == "") { predict2 = rs.getString(4); }
				else { predict2=predict2+","+rs.getString(4); }
	
				if(prob2 == "") { prob2 = rs.getString(5); }
				else { prob2=prob2+","+rs.getString(5); }
	
				if(fdr == "") { fdr = rs.getString(6); }
				else { fdr=fdr+","+rs.getString(6); }
	
				if(predict1 == "") { predict1 = rs.getString(7); }
				else { predict1=predict1+","+rs.getString(7); }
			}
			
			rs.close();
			pstmt.close();
			
			if(!(uniProtId.length()>0)){ uniProtId = "null"; }
			if(!(subst.length()>1)){ subst = "null"; }
			if(!(posProt.length()>0)){ posProt = "null"; }
			if(!(predict2.length()>0)){ predict2 = "null"; }
			if(!(prob2.length()>0)){ prob2 = "null"; }
			if(!(fdr.length()>0)){ fdr = "null"; }
			if(!(predict1.length()>0)){ predict1 = "null"; }
	
			String sql = "insert ignore into polyphen (rsid,proteinID,subs,posProt,pred,score,median,pph1Pred,fk_searchid) values (?,?,?,?,?,?,?,?,?)";
	
			PreparedStatement pstmt2 = annconnection.prepareStatement(sql);
			pstmt2.setString(1,snpid);
			pstmt2.setString(2,uniProtId);
			pstmt2.setString(3,posProt);
			pstmt2.setString(4,subst);
			pstmt2.setString(5,predict2);
			pstmt2.setString(6,prob2);
			pstmt2.setString(7,fdr);
			pstmt2.setString(8,predict1);
			pstmt2.setInt(9,this.getAnnsearchid());
			
			pstmt2.executeUpdate();
			pstmt2.close();
		} catch(SQLException e) {
			System.out.println("SQL Exception... Error in querying:");
			e.printStackTrace();
		}
	}
}