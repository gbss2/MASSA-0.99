import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import jade.lang.acl.ACLMessage;
import jade.lang.acl.UnreadableException;
import jade.core.AID;
import jade.core.behaviours.FSMBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.ParallelBehaviour;
import jade.lang.acl.MessageTemplate;

public class HGNCLocalAgent extends DBagent {
	private static final long serialVersionUID = 1L;

	/* Attributes */	
	BioDataLite hugodata;
	int input_length;
	int[] input;
	public String sn = "localhost";
	public String md = "massa";
	public String user = "massa";
	public String key = "@sdfghjkl!";

	/* Constructor */	
    public HGNCLocalAgent() {
    	this.setDBname("hugo");
   	 	this.setInformation("hugo");
   	 	this.hugodata = new BioDataLite();
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

		public waitRequest() {
			 MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
		}

		public void action() {
			ACLMessage msg = myAgent.receive(simplerequest_template);
			if (msg != null) {
				sender = msg.getSender();
				System.out.println("Agent " + getLocalName() +" received a REQUEST message from agent "+msg.getSender().getName());
				try {
					BioDataLite contentdata = (BioDataLite)msg.getContentObject();
					setAnnsearchid(contentdata.getSearchid());
					hugodata.setSearchid(getAnnsearchid());
					addBehaviour(new HUGOAction(sender,contentdata));
				} catch (UnreadableException e) { e.printStackTrace(); }
			} else { block(); }
		}
	}

	/* Agent action: access DB -> accessDBsnpLocal; send reply  */
    class HUGOAction extends FSMBehaviour {
		private static final long serialVersionUID = 1L;

		private static final String STATE_A = "A";
		private static final String STATE_B = "B";

		public AID partneragent;
		public int[] snpidlist;

		public HUGOAction(AID pa, BioDataLite bdlite){
			System.out.println("Agent " + getLocalName() +" executing request...");
			this.registerFirstState(new accessHUGO(bdlite), STATE_A);
			this.registerLastState(new SendReply(pa), STATE_B);
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
    class accessHUGO extends ParallelBehaviour {
		private static final long serialVersionUID = 1L;
		BioDataLite contentdata;

		public accessHUGO (BioDataLite bdl) {
			super(WHEN_ALL);
			contentdata = bdl;
		}

		public void onStart(){
			String query = "SELECT * FROM gene WHERE fk_searchid="+getAnnsearchid();
			try {
				Statement st = annconnection.createStatement();
				ResultSet rs = st.executeQuery(query);
	
			    while (rs.next()){
			    	this.addSubBehaviour(new mysqlLocalHUGOquery(rs.getString("gene_symbol")));
			    }

				st.close();
				rs.close();
			}catch (SQLException e){ e.printStackTrace(); }
		}
		
		public int onEnd(){
			System.out.println("Agent " + getLocalName() +" finished.");
			return super.onEnd();
		}
	}

	/* Agent action: Query DB  */
	class mysqlLocalHUGOquery extends OneShotBehaviour {
		private static final long serialVersionUID = 1L;
		private String genesymbol;

		public mysqlLocalHUGOquery(String gs) { genesymbol = gs; }
		public void action() { QueryHGNCLocal(genesymbol); }
	}

	
	/* LIST OF QUERIES */
	
	/* Agent action: Query SNP  */
	public void QueryHGNCLocal(String genesymbol) {
		String hugoId  = null;
		String geneSymbol  = null;
		String geneName  = null;
		String altGeneSymbols = null;
		String geneSyns = null;

		String geneTag = null;
		String geneFamily = null;
		String geneType = null;
		String geneGroup = null;

		String strsql = "select hgnc_id, " +
						"symbol, " +
						"name, " +
						"locus_type, " +
						"locus_group, " +
						"alias_symbol, " +
						"alias_name, " +
						"gene_family_id, " +
						"gene_family, " +
						"pubmed_id " +
						"FROM hugoDB_05_2018 " +
						"WHERE symbol = ? ;";

		try {
			PreparedStatement pstmt = conn.prepareStatement(strsql);
			pstmt.setString(1,genesymbol);
			ResultSet rs = pstmt.executeQuery();	

			while (rs.next()) {
				hugoId = rs.getString(1);
				geneSymbol = rs.getString(2);
				geneName = rs.getString(3);
				altGeneSymbols = rs.getString(6);
				geneSyns = rs.getString(7);
	
				geneTag = rs.getString(8);
				geneFamily = rs.getString(9);
				geneType = rs.getString(4);
				geneGroup = rs.getString(5);
			}
		
			String sql = "insert ignore into hugoDB (fk_searchid,hgnc_id,symbol,name,alias_symbol,alias_name," +
			"gene_family_id,gene_family,locus_type,locus_group) values (?,?,?,?,?,?,?,?,?,?)";
			PreparedStatement pstmt2 = annconnection.prepareStatement(sql);
			pstmt2.setInt(1,getAnnsearchid());
			pstmt2.setString(2,hugoId);
			pstmt2.setString(3,geneSymbol);
			pstmt2.setString(4,geneName);
			pstmt2.setString(5,altGeneSymbols);
			pstmt2.setString(6,geneSyns);
			pstmt2.setString(7,geneTag);
			pstmt2.setString(8,geneFamily);
			pstmt2.setString(9,geneType);
			pstmt2.setString(10,geneGroup);
			
			pstmt2.executeUpdate();
			pstmt2.close();
		} catch(SQLException e) {
			System.out.println("SQL Exception... Error in querying:");
			e.printStackTrace();
		}
	}
}