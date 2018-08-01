import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.Set;

import java.io.IOException;

import jade.lang.acl.ACLMessage;
import jade.lang.acl.UnreadableException;
import jade.core.AID;
import jade.core.behaviours.FSMBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.ParallelBehaviour;
import jade.lang.acl.MessageTemplate;

import BioData_Models.GeneOntology;

public class GOAgent extends DBagent{
	private static final long serialVersionUID = 1L;

	/* Attributes */
	public String sn = "mysql.ebi.ac.uk:4085";
	public String md = "go_latest";
	public String gouser = "go_select";
	public String gokey = "amigo";
	BioData godata;
		
	/* Constructor */
	public GOAgent() {
    	this.setDBname("go");
   	 	this.setInformation("gene ontology");
   	 	this.godata = new BioData("gene ontology");
    }

	/* Agent setup */
	protected void setup() {
        System.out.println("Agent "+getLocalName()+" started.");
        this.register();
   	 	this.dbConnect(sn, md, gouser, gokey);
        addBehaviour(new waitRequest());
    }

	/* Agent shutdown */
	
	
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
					BioData contentdata = (BioData)msg.getContentObject();
					addBehaviour(new GOAction(sender,contentdata));
				} catch (UnreadableException e) { e.printStackTrace(); }
			} else { block(); }
		}
	} 
	
	/* Agent action: access DB -> access DB remote; send reply  */
	class GOAction extends FSMBehaviour {
		private static final long serialVersionUID = 1L;

		private static final String STATE_A = "A";
		private static final String STATE_B = "B";
		
		public GOAction(AID pa,BioData bd){
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
	
	/* Agent action: parallel access to DB -> remote Access DB  */	
	class accessGO extends ParallelBehaviour {
		private static final long serialVersionUID = 1L;

		BioData contentdata;

		public accessGO (BioData bd) {
			super(WHEN_ALL);
			contentdata = bd;
		}
		
		public void onStart(){
			Set<String> keyset;
			keyset = contentdata.snp_gene.keySet();
			Iterator<String> itr = keyset.iterator();
			
			while (itr.hasNext()) {
				String snp  = itr.next();
				String gene = contentdata.snp_gene.get(snp);
				this.addSubBehaviour(new mysqlRemoteGOquery(snp,gene));
			}
		}
		
		public int onEnd(){
			System.out.println("Agent " + getLocalName() +" finished.");
			dbDisconnect();
			return super.onEnd();
		}
	}	

	/* Agent action: Query DB  */
	class mysqlRemoteGOquery extends OneShotBehaviour {
		private static final long serialVersionUID = 1L;

		private String snpid;
		private String genesymbol;
		private GeneOntology goobject;
		
		public mysqlRemoteGOquery(String s, String g) {
			this.snpid = s;
			this.genesymbol = g;
		}

		public void action() {
			System.out.println("Agent " + getLocalName() +" executing request for "+genesymbol+"...");
			goobject = QueryGOremote(genesymbol);
			goobject.setGenesymbol(genesymbol);
			goobject.setPolymorphismCode(snpid);

			synchronized(godata){
				godata.setGOList(goobject);
			}
		}	
	}
	

	/* LIST OF QUERIES */
	
	/* Agent action: Query SNP  */
	public GeneOntology QueryGOremote(String gs) {
		BioData bioobjects = new BioData("gene ontology");
		GeneOntology goobject = bioobjects.createGOInstance();
		
		String strsql = "Select distinct term_id," +
						" term_name," +
						" term_acc, " +
						"term_type, " +
						"gp_symbol, " +
						"pub_dbname, " +
						"species.ncbi_taxa_id, " +
						"species.id " +
						"FROM species, term_J_association_J_evidence_J_gene_product " +
						"WHERE gp_symbol = ? " +
						"AND gp_species_id = species.id AND species.ncbi_taxa_id = 9606;";
	
		try {
			PreparedStatement pstmt = conn.prepareStatement(strsql);
			pstmt.setString(1,gs);
			System.out.println("SQL:"+strsql);
	
			ResultSet rs = pstmt.executeQuery();	
		
			while (rs.next()) {
				String termName = rs.getString("term_name");
				String termType = rs.getString("term_type");
				
				System.out.println("GO Agent term type is "+termType+" term name is "+termName);
				if(termType.equals("biological_process")){
					goobject.setBioProcessItem(termName);
				}

				if(termType.equals("cellular_component")){
					goobject.setCelComponentItem(termName);
				} 

				if(termType.equals("molecular_function")){
					goobject.setMolFunctionItem(termName);
				}
			}
		
			rs.close();
			pstmt.close();
		} catch(SQLException e) {
			System.out.println("SQL Exception... Error in querying:");
			e.printStackTrace();
		}
		
		return goobject;
	}
}