import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Connection;
import java.util.List;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

import BioData_Models.GeneOntology;
import BioData_Models.HUGO;
import BioData_Models.OMIM;
import BioData_Models.PharmGKB;
import BioData_Models.Polymorphism;
import BioData_Models.UCSC;

import com.mysql.jdbc.PreparedStatement;

import jade.core.AID;
import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.FSMBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.UnreadableException;

public class CoordinatorAgent extends Agent {
	private static final long serialVersionUID = 1L;

	/* Attributes */
	private String[] service_types;
	private String[] info_types;
	private int analysistype;
	private int service_type;
	private Hashtable<AID,String> partner_list;
	private Hashtable<AID,Long> partner_tstart;
	private Hashtable<AID,Long> partner_tend;
	private long partner_exectime;

	public List<Integer> searchidlist;
	public List<BioData> biodatalist;
	public List<BioDataLite> biodatalitelist;
	public String fullFilename;
	private String myservicetype;
	private String myinformation;
	public boolean errflag;
	public int dbsearchid;
	
	public AID InterfaceAgent;

	public String sn = "localhost";
	public String dbname = "massa";
	public String dbUser = "massa";
	public String dbKey = "@sdfghjkl!";
	MySQLcon myannconnection;
	public Connection connection;
	
	/* Constructor */
	public CoordinatorAgent() {
		partner_list = new Hashtable<AID,String>();
		biodatalist  =  new ArrayList<BioData>();
		biodatalitelist=new ArrayList<BioDataLite>();
		searchidlist =  new ArrayList<Integer>();
		partner_tstart = new Hashtable<AID,Long>();
		partner_tend = new Hashtable<AID,Long>();
		partner_exectime = 0;
		
		this.setMyservicetype("coordinator");
		this.setMyinformation("coordinator");

		service_types = new String[2];
		service_types[0] = "database_search";
		service_types[1] = "singleEnrichment";
		
		info_types    = new String[13];
		info_types[0] = "sample";
		info_types[1] = "snp";
		info_types[2] = "pharmgkb";
		info_types[3] = "snp to gene";
		info_types[4] = "gene ontology";
		info_types[5] = "ucsc";
		info_types[6] = "omim";
		info_types[7] = "hugo";
		info_types[8] = "gwas";
		info_types[9] = "polyphen";
		info_types[10] = "provean";
		info_types[11] = "reactome";
		info_types[12] = "singleEnrichment";
		
		errflag = false;
	}

	/* Agent setup */
	protected void setup() {
		System.out.println("Agent "+getLocalName()+" started.");
		register();
		
		myannconnection = new MySQLcon(sn, dbname, dbUser, dbKey);
		connection = myannconnection.mysqlConnect();

		addBehaviour(new waitMsg());
	}

	/* Agent shutdown */
	protected void takeDown() {
		myannconnection.mysqlDisconnect();
		System.out.println("Agent" + getLocalName() + " shutdown.");
	}

	/* Agent register */
    protected void register(){
    	try {
    		DFAgentDescription dfd = new DFAgentDescription();
    		dfd.setName(getAID());
			System.out.println("Agent "+getLocalName()+" registering service type \""+this.getMyServiceType()+"\" and info "+this.getMyinformation()+" with DF");

			ServiceDescription sd = new ServiceDescription();
			sd.setName(this.getMyinformation());
			sd.setType(this.getMyServiceType());   		
			dfd.addServices(sd);	
    		DFService.register(this, dfd);
    	} catch (FIPAException fe) { fe.printStackTrace(); }
    }

	
	/* BEHAVIOURS */
	
	/* CHAT AND ACTION FLOW BEHAVIOUR */
	class waitMsg extends CyclicBehaviour {
		private static final long serialVersionUID = 1L;

		private String previous_info_type;
		private AID sender;
		private String  msgperformative;
		private String setinfotype;
		private String servicetype;

		public waitMsg() {}
		
		public void action() {
			ACLMessage msg = myAgent.receive();

			if (msg != null) {
				sender = msg.getSender();
				msgperformative = ACLMessage.getPerformative(msg.getPerformative());

				if(partner_list.containsKey(sender)){
					
					previous_info_type = partner_list.get(sender);
					partner_list.remove(sender);
					partner_tend.put(sender, System.currentTimeMillis() );
					setpartnerexectime(sender);
					
					System.out.println("Agent " + getLocalName() +" received a REPLY from agent "+sender.getName()+" info type was "+previous_info_type);
					
					try {
						if(getServicetype() == 0 && getAnalysistype() == 1) {
							BioDataLite contentdata = (BioDataLite)msg.getContentObject();
							
							if(!searchidlist.contains(contentdata.getSearchid())){
								searchidlist.add(contentdata.getSearchid());
							}
							System.out.println("Partnerlist size is "+partner_list.size());
							if(partner_list.size() == 0){
								BioDataLite combinedbdl = new BioDataLite();
								Iterator<Integer> itr = searchidlist.iterator();
								while(itr.hasNext()) {
								    Integer sid = (Integer)itr.next();
								    combinedbdl.setSearchidItem(sid);
								}    
								addBehaviour(new SendReply(InterfaceAgent,combinedbdl));
							}
						}

						if(getAnalysistype() == 2) {
							BioData contentdata = (BioData)msg.getContentObject();
							addBehaviour(new SendReply(InterfaceAgent,contentdata));
						}

						if(getServicetype() == 0 && getAnalysistype() == 3) {
							BioDataLite contentdata = (BioDataLite)msg.getContentObject();
							
							if(!searchidlist.contains(contentdata.getSearchid())){
								searchidlist.add(contentdata.getSearchid());
							}
							
							if(previous_info_type == "snp"){
								setinfotype = info_types[4]; // GO
								addBehaviour(new FindAndSend(service_types[0],setinfotype,"REQUEST",contentdata));

								setinfotype = info_types[6]; // OMIM
								addBehaviour(new FindAndSend(service_types[0],setinfotype,"REQUEST",contentdata));

//								setinfotype = info_types[2]; // PGKB
//								addBehaviour(new FindAndSend(service_types[0],setinfotype,"REQUEST",contentdata));
							
								setinfotype = info_types[7]; // HGNC
								addBehaviour(new FindAndSend(service_types[0],setinfotype,"REQUEST",contentdata));
								
								setinfotype = info_types[11]; // REACTOME
								addBehaviour(new FindAndSend(service_types[0],setinfotype,"REQUEST",contentdata));
							}

							System.out.println("Partnerlist size is "+partner_list.size());
							if(partner_list.size() == 0 && previous_info_type != "snp"){
								BioDataLite combinedbdl = new BioDataLite();
								Iterator<Integer> itr = searchidlist.iterator();

								while(itr.hasNext()) {
								    Integer sid = (Integer)itr.next();
								    combinedbdl.setSearchidItem(sid);
								}    
								addBehaviour(new SendReply(InterfaceAgent,combinedbdl));
							}
						}
						
						if(getAnalysistype() == 4) {
							BioData contentdata = (BioData)msg.getContentObject();
							biodatalist.add(contentdata);
							
							if(previous_info_type == "snp"){
//								setinfotype = info_types[2]; // PGKB
//								BioData objecttosend = new BioData();
//								objecttosend.setSnpGene(contentdata.createSnpGeneAssoc());
//								addBehaviour(new FindAndSend(service_types[0],setinfotype,"REQUEST",objecttosend));
								
								setinfotype = info_types[4]; // GO
								BioData objecttogo = new BioData();
								objecttogo.setSnpGene(contentdata.createSnpGeneAssoc());
								addBehaviour(new FindAndSend(service_types[0],setinfotype,"REQUEST",objecttogo));

								setinfotype = info_types[6]; // OMIM
								BioData objecttoomim = new BioData();
								objecttoomim.setSnpGene(contentdata.createSnpGeneAssoc());
								addBehaviour(new FindAndSend(service_types[0],setinfotype,"REQUEST",objecttoomim));
								
								setinfotype = info_types[7]; // HGNC
								BioData objecttohugo = new BioData();
								objecttohugo.setSnpGene(contentdata.createSnpGeneAssoc());
								addBehaviour(new FindAndSend(service_types[0],setinfotype,"REQUEST",objecttohugo));
							}

							System.out.println("Partnerlist size is "+partner_list.size());

							if(partner_list.size() == 0 && previous_info_type != "snp"){
								System.out.println("Sending Combined Annotation");
								addBehaviour(new combineAnnotationAndSend(InterfaceAgent));
							}
						}
						
						if(getServicetype() == 1) {
							
							System.out.println("Agent " + getLocalName() +" sending INFORM to agent "+sender.getName() + "service type = " + getServicetype());
							BioDataLite contentdata = (BioDataLite)msg.getContentObject();
							contentdata.setServicetype(getServicetype());
							addBehaviour(new SendReply(InterfaceAgent,contentdata));
							
						}
						
					} catch (UnreadableException e) { e.printStackTrace(); }
				} else { 
					if(msgperformative == "REQUEST"){
						System.out.println("Agent " + getLocalName() +" received a "+msgperformative+" from agent "+sender.getName());
						InterfaceAgent = msg.getSender();

						try {

							
							if (msg.getContentObject() instanceof BioData) {
								
								System.out.println("Agent " + getLocalName() +" infotype is "+setinfotype);
								System.out.println("Agent " + getLocalName() +" content is BioData.");

								BioData contentdata = (BioData)msg.getContentObject();
								setinfotype = info_types[1]; // dbSNP
								setAnalysistype(contentdata.getAnalysistype());
								addBehaviour(new checkInputAction(contentdata,setinfotype));
								
								setinfotype = info_types[5]; // UCSC
								setAnalysistype(contentdata.getAnalysistype());
								addBehaviour(new checkInputAction(contentdata,setinfotype));
							}
							if (msg.getContentObject() instanceof BioDataLite) {
								

								BioDataLite contentdatalite = (BioDataLite)msg.getContentObject();
								
								System.out.println("Agent " + getLocalName() +" serviceType is "+contentdatalite.getServicetype());
								System.out.println("Agent " + getLocalName() +" content is BioDataLite.");
								
								if(contentdatalite.getServicetype() == 0) {
									
									System.out.println("Agent " + getLocalName() +" infotype is "+setinfotype);

									setinfotype = info_types[1]; // dbSNP
									setAnalysistype(contentdatalite.getAnalysistype());
									addBehaviour(new checkInputAction(contentdatalite,setinfotype));
									
									setinfotype = info_types[5]; // UCSC
									setAnalysistype(contentdatalite.getAnalysistype());
									addBehaviour(new checkInputAction(contentdatalite,setinfotype));
	
									setinfotype = info_types[8]; // GWAS
									setAnalysistype(contentdatalite.getAnalysistype());
									addBehaviour(new checkInputAction(contentdatalite,setinfotype));
									
									setinfotype = info_types[9]; // PolyPhen
									setAnalysistype(contentdatalite.getAnalysistype());
									addBehaviour(new checkInputAction(contentdatalite,setinfotype));		
									
									setinfotype = info_types[10]; // Provean
									setAnalysistype(contentdatalite.getAnalysistype());
									addBehaviour(new checkInputAction(contentdatalite,setinfotype));
									
									setinfotype = info_types[2]; // PGKB
									setAnalysistype(contentdatalite.getAnalysistype());
									addBehaviour(new checkInputAction(contentdatalite,setinfotype));								
								}
								
								if(contentdatalite.getServicetype() == 1) {
									
									setinfotype = info_types[12];
									System.out.println("Agent " + getLocalName() +" infotype is "+setinfotype);
									System.out.println("Agent " + getLocalName() +" received file path "+ contentdatalite.getFullFilename());
									setAnalysistype(contentdatalite.getAnalysistype());
									setServicetype(contentdatalite.getServicetype());
									setFullFilename(contentdatalite.getFullFilename());
									addBehaviour(new FindAndSend(service_types[1],setinfotype,"REQUEST",contentdatalite));
									
								}
							}
						} catch (UnreadableException e) { e.printStackTrace(); }
					}else{
						System.out.println("Agent " + getLocalName() +" does not know agent "+sender.getName());
					}
				}
			} else {
				if(partner_exectime > 0){ checkdelay(); }
				block();
			}
		}

		public void setpartnerexectime(AID a){
			long exectime = partner_tend.get(a) - partner_tstart.get(a);
			if(exectime > partner_exectime){ partner_exectime = exectime; }
		}
		
		public void checkdelay(){
			Map<AID, String> map = partner_list;
			Iterator<Map.Entry<AID, String>> it = map.entrySet().iterator();

			while (it.hasNext()) {
				Map.Entry<AID, String> entry = it.next();
				Long remaintime = System.currentTimeMillis() - partner_tstart.get(entry.getKey());
				if (remaintime > (partner_exectime*2)) {
					System.out.println("Agent " + entry.getKey().getName() +" is delayed");
				}
			}
		}
	}
	
	
	/* CHECK INPUT ACTION - FINITE STATE MACHINE BEHAVIOUR */
	class checkInputAction extends FSMBehaviour {
		private static final long serialVersionUID = 1L;

		private static final String STATE_A = "A";
		private static final String STATE_B = "B";
		private static final String STATE_C = "C";
		
		public checkInputAction(BioDataLite bd, String ift){
			this.registerFirstState(new checkServiceType(bd), STATE_A);
			this.registerState(new annotationDBconnect(bd), STATE_B);
			System.out.println("Threads ativas:"+Thread.getAllStackTraces().size());
			this.registerLastState(new FindAndSend(service_types[0],ift,"REQUEST",bd), STATE_C);
			this.registerDefaultTransition(STATE_A, STATE_B);
			this.registerDefaultTransition(STATE_B, STATE_C);
		}

		
		public checkInputAction(BioData bd, String ift){
			this.registerFirstState(new checkInfoType(bd), STATE_A);
			this.registerLastState(new FindAndSend(service_types[0],ift,"REQUEST",bd), STATE_B);
			this.registerDefaultTransition(STATE_A, STATE_B);
		}

		public int onEnd() { return super.onEnd(); }
	}
	
	/* CHECK INPUT ACTION - SUB BEHAVIOURS */

	
	/* CHECK INFO TYPE */
	class checkServiceType extends OneShotBehaviour {
		private static final long serialVersionUID = 1L;
		
		private BioDataLite bdlite;
		private BioData bd;		
		
		public checkServiceType(BioDataLite b){
			this.bdlite = b;
		}

		public checkServiceType(BioData b){
			this.bd = b;
		}
		
		public void action(){
			
			if(bdlite.getServicetype()==0){
				
				addBehaviour(new checkInfoType(bdlite));
				
			}
			
			if(bdlite.getServicetype()==1){
				
				return;
				
			}
		}
		
	}

	
	/* CHECK INFO TYPE */
	class checkInfoType extends OneShotBehaviour {
		private static final long serialVersionUID = 1L;

		String[] rsarray;
		int[] idarray;
		private BioDataLite bdlite;
		private BioData bd;
		
		public checkInfoType(BioDataLite b){
			this.bdlite = b;
		}

		public checkInfoType(BioData b){
			this.bd = b;
		}

		public void action(){
			if(getAnalysistype()==1 || getAnalysistype()==3){
				rsarray = bdlite.getSnpRsList();
			}
			if(getAnalysistype()==2 || getAnalysistype()==4){
				rsarray = bd.getSnpRsList();
			}
			
			idarray = new int[rsarray.length];
			if( rsarray[0].contains("rs")){
				for (int i = 0; i < rsarray.length; ++i) {
					idarray[i] = Integer.parseInt(rsarray[i].substring(2));
				}
			}else{
				for (int i = 0; i < rsarray.length; ++i) {
					idarray[i] = Integer.parseInt(rsarray[i]);
				}
			}

			if(getAnalysistype()==1 || getAnalysistype()==3){
				bdlite.setSnpIdList(idarray);
			}
			if(getAnalysistype()==2 || getAnalysistype()==4){
				bd.setSnpIdList(idarray);
			}
		}
	}	
	
	/* CONNECT TO ANNOTATION DB AND GENERATE SEARCH ID */	
	class annotationDBconnect extends OneShotBehaviour {
		private static final long serialVersionUID = 1L;

		private BioDataLite bdlite;
		
		public annotationDBconnect(BioDataLite b){
			this.bdlite = b;
		}
		
		public void action(){
			String sql = "insert into search (searchid) values (null)";
			
			try {
				PreparedStatement stm = (PreparedStatement) connection.prepareStatement(sql);
				stm.executeUpdate(sql, Statement.RETURN_GENERATED_KEYS);
				ResultSet rs = stm.getGeneratedKeys();

				if(rs.next()) 
				{
					dbsearchid = rs.getInt(1);
				}
				
				bdlite.setSearchid(dbsearchid);
			
				stm.close();
				rs.close();
				
			} catch(SQLException e) {
				System.out.println("SQL Exception... Error inserting value in search annotation table.");
				e.printStackTrace();
				
				errflag = true;
			}
		}

		public int onEnd() {
			return 0;
		}		
	}	
	
	/* SEARCH PARTNER BEHAVIOUR */
	class FindAndSend extends OneShotBehaviour {
		private static final long serialVersionUID = 1L;

		private String type_of_service;
		private String type_of_info;
		private AID[] providers;
		private ACLMessage msg;
		private AID     msgreceiver;
		private String  msgperformative;
		private BioDataLite msgcontentlite;
		private BioData msgcontent;
		
		
		public FindAndSend(String st, String it, String p, BioDataLite d) {
			type_of_service = st;
			type_of_info    = it;
			msgperformative = p;
			msgcontentlite = d;
			
			if(errflag){ this.done(); }
		}

		public FindAndSend(String st, String it, String p, BioData d) {
			type_of_service = st;
			type_of_info    = it;
			msgperformative = p;
			msgcontent = d;
			
			if(errflag){ this.done(); }
		}
		
		public void action() {
			System.out.println("Agent "+getLocalName()+" searching for service type \""+type_of_service+"\" and information \""+type_of_info+"\" ");

			try {
				DFAgentDescription template = new DFAgentDescription();
				ServiceDescription templateSd = new ServiceDescription();
				templateSd.setType(type_of_service);
				templateSd.setName(type_of_info);
				template.addServices(templateSd);

				DFAgentDescription[] results = DFService.search(myAgent, template);
				if (results.length > 0) {
					providers = new AID[results.length];
					for (int i = 0; i < results.length; ++i) {
						DFAgentDescription dfd = results[i];
						providers[i] = dfd.getName();
					}
					
					System.out.println("Agent "+getLocalName()+" found "+providers.length+" providers.");
				} else {
					System.out.println("Agent "+getLocalName()+" did not find any agent for the required information: "+type_of_info);
					this.done();
				}
			} catch (FIPAException fe) { fe.printStackTrace(); }
			
			if(providers != null) {
				Random rand;
				int rindex;
				
				rand = new Random();
				
				rindex = rand.nextInt(providers.length -1 - 0 + 1) + 0;
				msgreceiver = providers[rindex];
				
				while(partner_list.containsKey(msgreceiver)){
					rindex = rand.nextInt(providers.length -1 - 0 + 1) + 0;
					msgreceiver = providers[rindex];
				}
				
				try {
					if (msgperformative == "REQUEST") {
						msg = new ACLMessage(ACLMessage.REQUEST);
						System.out.println("... sending "+ACLMessage.getPerformative(msg.getPerformative())+" to agent("+rindex+"): \""+msgreceiver.getName());
					}
					msg.addReceiver(msgreceiver);
					msg.setLanguage("English");

					if(getAnalysistype()==1 || getAnalysistype()==3){
						msg.setContentObject(msgcontentlite);	
					}
					if(getAnalysistype()==2 || getAnalysistype()==4){
						msg.setContentObject(msgcontent);
					}

					myAgent.send(msg);
	
					synchronized(partner_list){
						partner_list.put(msgreceiver,type_of_info);
						partner_tstart.put(msgreceiver, System.currentTimeMillis() );
					}
				} catch (IOException e) { e.printStackTrace(); }
			}
		}
	}
	
	/* MERGE ANNOTATION DATA */
	class combineAnnotationAndSend extends OneShotBehaviour {
		private static final long serialVersionUID = 1L;

		private BioData combinedbd;
		private BioData combinedlists;
		private AID receiver;
		
		public combineAnnotationAndSend(AID tosend){
			this.receiver = tosend;
		}

		public void action(){
			System.out.println("Agent " + getLocalName()+" is combining results... ");
			this.combinedlists = new BioData();			
			Iterator<BioData> itr = biodatalist.iterator();

			while(itr.hasNext()) {
				BioData bd = (BioData)itr.next();
				
				if(bd.getPolymorphismList() != null){
					for (Polymorphism p : bd.getPolymorphismList()){
						combinedlists.setPolymorphismList(p);
					}
				}

				if(bd.getUCSCList() != null){
					for (UCSC us: bd.getUCSCList()){
						combinedlists.setUCSCList(us);
					}
				}
				
				if(bd.getPharmgkbList() != null){
					for (PharmGKB ph : bd.getPharmgkbList()){
						combinedlists.setPharmgkbList(ph);
					}
				}
				
				if(bd.getGOList() != null){
					for (GeneOntology go: bd.getGOList()){
						combinedlists.setGOList(go);
					}
				}

				if(bd.getOMIMList() != null){
					for (OMIM om: bd.getOMIMList()){
						combinedlists.setOMIMList(om);
					}
				}

				if(bd.getHUGOList() != null){
					for (HUGO hg: bd.getHUGOList()){
						combinedlists.setHUGOList(hg);
					}
				}
			}

			combinedbd = new BioData();
			Iterator<Polymorphism> itrp = combinedlists.getPolymorphismList().iterator();

			while(itrp.hasNext()) {
				Polymorphism pol = (Polymorphism)itrp.next();
				PharmGKB ph = combinedlists.getPharmGKBObject(pol.getPolymorphismCode(),combinedlists.getPharmgkbList());
				pol.setPathwayList(ph.getPathwayList());
				pol.setDrugList(ph.getDrugList());
				pol.setDiseaseList(ph.getDiseaseList());
				pol.setGenexList(ph.getGenexList());
				
				GeneOntology go = combinedlists.getGOobject(pol.getPolymorphismCode(),combinedlists.getGOList());
				pol.setMolFunctionList(go.getMolFunction());
				pol.setBioProcessList(go.getBioProcess());
				pol.setCelComponentList(go.getCelComponent());
				
				UCSC us = combinedlists.getUCSCobject(pol.getPolymorphismCode(),combinedlists.getUCSCList());
				pol.setStrand(us.getStrand());
				pol.setRefUCSC(us.getRefUCSC());
				pol.setObsGen(us.getObsGen());
				pol.setUcscClass(us.getUcscClass());
				pol.setUcscFunc(us.getUcscFunc());
				
				OMIM om = combinedlists.getOMIMobject(pol.getPolymorphismCode(),combinedlists.getOMIMList());
				pol.setCytoloc(om.getCytoloc());
				pol.setGenestatus(om.getGenestatus());
				pol.setDisorderList(om.getDisorder());
				pol.setMimIDList(om.getMimID());
				pol.setGenemapmethods(om.getGenemapmethods());
				pol.setInheritanceList(om.getInheritance());
				pol.setPhenoMapMethodsList(om.getPhenoMapMethods());
				pol.setCommentsList(om.getComments());
				
				HUGO hg = combinedlists.getHUGOobject(pol.getPolymorphismCode(),combinedlists.getHUGOList());
				pol.setHgncId(hg.getHgncId());
				pol.setHgGeneName(hg.getHgGeneName());
				pol.setGeneSynonyms(hg.getGeneSynonyms());
				pol.setLocusType(hg.getLocusType());
				pol.setLocusGroup(hg.getLocusGroup());
				pol.setGeneFamilyTag(hg.getGeneFamilyTag());
				pol.setGeneFamily(hg.getGeneFamily());
				pol.setSpecialistDBLinks(hg.getSpecialistDBLinks());
				pol.setLocusSpecDB(hg.getLocusSpecDB());
				pol.setEnzymeId(hg.getEnzymeId());
				pol.setEntrezId(hg.getEntrezId());
				pol.setEnsemblId(hg.getEnsemblId());
				pol.setPubMedIds(hg.getPubMedIds());
				pol.setRefSeqIds(hg.getRefSeqIds());
				pol.setCCDSIds(hg.getCCDSIds());
				pol.setVegaIds(hg.getVegaIds());
				pol.setUniProtId(hg.getUniProtId());
				pol.setMouseGdbId(hg.getMouseGdbId());
				pol.setRatGdbId(hg.getRatGdbId());
				
				combinedbd.setPolymorphismList(pol);
			}
			addBehaviour(new SendReply(receiver,combinedbd));
		}
	}
	
	/* SEND ANNOTATION DATA TO INTERFACE */
	class SendReply extends OneShotBehaviour {
		private static final long serialVersionUID = 1L;

		private AID     msgreceiver;
		private String  msgperformative;
		private BioDataLite msgcontentlite;
		private BioData msgcontent;
		private ACLMessage msg;

		public SendReply(AID p, BioDataLite d) {
			msgreceiver     = p;
			msgcontentlite  = new BioDataLite();
			msgcontentlite  = d;
			msgperformative = "INFORM";
		}

		public SendReply(AID p, BioData d) {
			msgreceiver     = p;
			msgcontent      = new BioData();
			msgcontent      = d;
			msgperformative = "INFORM";
		}
		
		public void action() {
			try {
				System.out.println("... sending "+msgperformative+" to agent: \""+msgreceiver.getName());
				msg = new ACLMessage(ACLMessage.INFORM);
				msg.addReceiver(msgreceiver);
				msg.setLanguage("English");

				if(getAnalysistype()==1 || getAnalysistype()==3){
					msg.setContentObject(msgcontentlite);	
				}
				if(getAnalysistype()==2 || getAnalysistype()==4){
					msg.setContentObject(msgcontent);
				}
				myAgent.send(msg);
			} catch (IOException e) { e.printStackTrace(); }
		}
	}

	
	/* DEPRECATED BEHAVIUORS */	

	/* SEND MESSAGE TO SERVICE PROVIDER */	
	class SendMsgtoProvider extends OneShotBehaviour {
		private static final long serialVersionUID = 1L;

		private AID     msgreceiver;
		private String  msgperformative;
		private BioDataLite msgcontent;
		private ACLMessage msg;

		public SendMsgtoProvider(AID p, String f, BioDataLite d) {
			msgreceiver     = p;
			msgperformative = f;
			msgcontent      = new BioDataLite();
			msgcontent      = d;
		}
		
		public void action() {
			try {
				System.out.println("... sending "+msgperformative+" to agent: \""+msgreceiver.getName());
				if (msgperformative == "REQUEST") {
					msg = new ACLMessage(ACLMessage.REQUEST);
				}
				msg.addReceiver(msgreceiver);
				msg.setLanguage("English");
				msg.setContentObject(msgcontent);
				myAgent.send(msg);
			} catch (IOException e) { e.printStackTrace(); }
		}
	}


	/* GETS AND SETS LIST */

	public String getMyServiceType() {
		return myservicetype;
	}

	public void setMyservicetype(String myservicetype) {
		this.myservicetype = myservicetype;
	}

	public String getMyinformation() {
		return myinformation;
	}

	public void setMyinformation(String myinformation) {
		this.myinformation = myinformation;
	}

	public int getAnalysistype() {
		return analysistype;
	}

	public void setAnalysistype(int analysistype) {
		this.analysistype = analysistype;
	}
	
	public int getServicetype() {
		return service_type;
	}

	public void setServicetype(int service_type) {
		this.service_type = service_type;
	}
	
	public String getFullFilename() {
		return fullFilename;
	}

	public void setFullFilename(String fullFilename) {
		this.fullFilename = fullFilename;
	}
}