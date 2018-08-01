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

public class reactomeLocalAgent extends DBagent{
	private static final long serialVersionUID = 1L;

	/* Attributes */
	BioDataLite reactomedata;
	int input_length;
	int[] input;
	public String sn = "localhost";
	public String md = "massa";
	public String user = "massa";
	public String key = "@sdfghjkl!";

	/* Constructor */
	public reactomeLocalAgent() {
		this.setDBname("reactome");
		this.setInformation("reactome");
		this.reactomedata = new BioDataLite();
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
				sender = msg.getSender();
				System.out.println("Agent " + getLocalName() +" received a REQUEST message from agent "+msg.getSender().getName());
				try {
					BioDataLite contentdata = (BioDataLite)msg.getContentObject();
					setAnnsearchid(contentdata.getSearchid());
					reactomedata.setSearchid(getAnnsearchid());
					addBehaviour(new reactomeAction(sender,contentdata));
				} catch (UnreadableException e) { e.printStackTrace(); }
			} else { block(); }
		}
	}
	
	/* Agent action: access DB -> accessDBsnpLocal; send reply  */	
	class reactomeAction extends FSMBehaviour {
		private static final long serialVersionUID = 1L;

		private static final String STATE_A = "A";
		private static final String STATE_B = "B";

		public reactomeAction(AID pa, BioDataLite bdlite){
			System.out.println("Agent " + getLocalName() +" executing request...");
			this.registerFirstState(new accessreactome(bdlite), STATE_A);
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
	class accessreactome extends ParallelBehaviour {
		private static final long serialVersionUID = 1L;

		BioDataLite contentdata;

		public accessreactome (BioDataLite bdl) {
			super(WHEN_ALL);
			contentdata = bdl;
		}

		public void onStart(){
			String query = "SELECT * FROM gene WHERE fk_searchid="+getAnnsearchid();
			try {
				Statement st = annconnection.createStatement();
				ResultSet rs = st.executeQuery(query);
	
			    while (rs.next()){
			    	this.addSubBehaviour(new mysqlLocalreactomequery(rs.getString("gene_symbol")));
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
	class mysqlLocalreactomequery extends OneShotBehaviour {
		private static final long serialVersionUID = 1L;

		private String genesymbol;

		public mysqlLocalreactomequery(String gs) { genesymbol = gs; }
		public void action() { Queryreactome(genesymbol); }
	}
	

	/* LIST OF QUERIES */
	
	/* Agent action: Query REACTOME  */
	public void Queryreactome(String genesymbol) {
		String pathway = "";
		String strsql = "SELECT reactome_02_2018.pathway, " +
						"reactome_02_2018.gene " +
						"FROM reactome_02_2018 " +
						"WHERE " +
						"reactome_02_2018.gene = ? ;";
		
		try {
			PreparedStatement pstmt = conn.prepareStatement(strsql);
			pstmt.setString(1,genesymbol);
			ResultSet rs = pstmt.executeQuery();
				
			while (rs.next()) {
				if(pathway == "") { pathway = rs.getString(1); }
				else { pathway=pathway+";"+rs.getString(1); }
			}
			
			rs.close();
			pstmt.close();
			
			if(!(pathway.length()>0)){ pathway = "null"; }
	
			String sql = "insert ignore into reactome (genesymbol,pathway,fk_searchid) values (?,?,?)";
			PreparedStatement pstmt2 = annconnection.prepareStatement(sql);
	
			pstmt2.setString(1,genesymbol);
			pstmt2.setString(2,pathway);
			pstmt2.setInt(3,this.getAnnsearchid());
			
			pstmt2.executeUpdate();
			pstmt2.close();
		} catch(SQLException e) {
			System.out.println("SQL Exception... Error in querying:");
			e.printStackTrace();
		}
	}
}