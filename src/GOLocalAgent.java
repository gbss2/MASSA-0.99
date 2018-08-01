import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import java.io.IOException;

import jade.lang.acl.ACLMessage;
import jade.lang.acl.UnreadableException;
import jade.core.AID;
import jade.core.behaviours.FSMBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.ParallelBehaviour;
import jade.lang.acl.MessageTemplate;

public class GOLocalAgent extends DBagent{
	private static final long serialVersionUID = 1L;

	/* Attributes */
	public String sn = "localhost";
	public String md = "massa";
	public String gouser = "massa";
	public String gokey = "@sdfghjkl!";

	BioDataLite godata;

	/* Constructor */
	public GOLocalAgent() {
    	this.setDBname("go");
   	 	this.setInformation("gene ontology");
     	this.godata = new BioDataLite();
    }

	/* Agent setup */
	protected void setup() {
        System.out.println("Agent "+getLocalName()+" started.");
        this.register();
   	 	this.dbConnect(sn, md, gouser, gokey);
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
					godata.setSearchid(getAnnsearchid());
					addBehaviour(new GOAction(sender,contentdata));
				} catch (UnreadableException e) { e.printStackTrace(); }
			} else { block(); }
		}
	} 

	/* Agent action: access DB -> accessDBsnpLocal; send reply  */
	class GOAction extends FSMBehaviour {
		private static final long serialVersionUID = 1L;

		private static final String STATE_A = "A";
		private static final String STATE_B = "B";

		public GOAction(AID pa,BioDataLite bd){
			System.out.println("Agent " + getLocalName() +" executing request...");
			this.registerFirstState(new accessGO(bd), STATE_A);
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
		private ACLMessage msg;

		public SendReply(AID p) {
			msgreceiver     = p;
			msgperformative = "INFORM";
		}

		public void action() {
			try {
				System.out.println("... sending "+msgperformative+" to agent: \""+msgreceiver.getName());
				msg = new ACLMessage(ACLMessage.INFORM);
				msg.addReceiver(msgreceiver);
				msg.setLanguage("English");
				msg.setContentObject(godata);
				myAgent.send(msg);
			} catch (IOException e) { e.printStackTrace(); }
		}
	}
	
	/* Agent action: parallel access to DB -> remoteAccessDBsnpLocal  */
	class accessGO extends ParallelBehaviour {
		private static final long serialVersionUID = 1L;

		BioDataLite contentdata;

		public accessGO (BioDataLite bd) {
			super(WHEN_ALL);
			contentdata = bd;
		}

		public void onStart(){
			String query = "SELECT * FROM gene WHERE fk_searchid="+getAnnsearchid();

			try {
				Statement st = annconnection.createStatement();
				ResultSet rs = st.executeQuery(query);
	
			    while (rs.next())
			    {
			    	this.addSubBehaviour(new mysqlLocalGOquery(rs.getString("gene_symbol")));
			    }
	
				st.close();
				rs.close();
			} catch (SQLException e){ e.printStackTrace(); }
		}

		public int onEnd(){
			System.out.println("Agent " + getLocalName() +" finished.");
			return super.onEnd();
		}
	}

	/* Agent action: Query DB  */
	class mysqlLocalGOquery extends OneShotBehaviour {
		private static final long serialVersionUID = 1L;

		private String genesymbol;

		public mysqlLocalGOquery(String g) { this.genesymbol = g; }

		public void action() { QueryGOLocal(genesymbol); }
	}


	/* LIST OF QUERIES */
	
	/* Agent action: Query GO  */
	public void QueryGOLocal(String gs) {
		String bp = "";
		String cc = "";
		String mf = "";
		
		List<String> bpl;
		List<String> ccl;
		List<String> mfl;
		
		bpl = new ArrayList<String>();
		ccl = new ArrayList<String>();
		mfl = new ArrayList<String>();
		
		String strsql = "SELECT  gene_product.symbol, " +
						"term.term_type, " +
						"term.name " +
						"FROM gene_product " +
					//	"INNER JOIN species ON (gene_product.species_id=species.id) " +
						"INNER JOIN association ON (gene_product.id=association.gene_product_id) " +
						"INNER JOIN term ON (association.term_id=term.id) " +
						"WHERE gene_product.symbol = ? " +
						"AND gene_product.species_id = '508591';";

		try {
			PreparedStatement pstmt = conn.prepareStatement(strsql);
			pstmt.setString(1,gs);
			ResultSet rs = pstmt.executeQuery();	

			while (rs.next()) {
				String termType = rs.getString("term_type");
				String termName = rs.getString("name");

				if(termType.equals("biological_process")){
					if(!termName.equals("")){
						if(!bpl.contains(termName)){
							bp = bp+";"+termName;
							bpl.add(termName);
						}
					}
				}

				if(termType.equals("cellular_component")){
					if(!termName.equals("")){
						if(!ccl.contains(termName)){
							cc = cc+";"+termName;
							ccl.add(termName);
						}
					}
				} 

				if(termType.equals("molecular_function")){
					if(!termName.equals("")){
						if(!mfl.contains(termName)){
							mf = mf+";"+termName;
							mfl.add(termName);
						}
					}
				}
			}

			rs.close();
			pstmt.close();

			if(bp.length()>0){ bp = bp.substring(1); }
			else { bp = "null"; }

			if(cc.length()>0){ cc = cc.substring(1); }
			else{ cc = "null"; }

			if(mf.length()>0){ mf = mf.substring(1); }
			else{ mf = "null"; }
			
			String upsql = "insert ignore into geneOntology (gp_symbol,molFunction,celComp,bioProcess,fk_searchid) values (?,?,?,?,?)";
			
			PreparedStatement pstm = annconnection.prepareStatement(upsql);
			pstm.setString(1,gs);
			pstm.setString(2,mf);
			pstm.setString(3,cc);
			pstm.setString(4,bp);
			pstm.setInt(5,this.getAnnsearchid());
			
			pstm.executeUpdate();
		} catch(SQLException e) {
			System.out.println("SQL Exception... Error in querying:");
			e.printStackTrace();
		}
	}
}