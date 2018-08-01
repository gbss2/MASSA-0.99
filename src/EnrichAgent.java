import jade.content.ContentException;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.basic.Action;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.FSMBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPANames;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.JADEAgentManagement.JADEManagementOntology;
import jade.domain.JADEAgentManagement.ShutdownPlatform;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;
import jade.wrapper.ContainerController;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import jsc.contingencytables.ContingencyTable2x2;
import jsc.contingencytables.FishersExactTest;
import BioData_Models.Polymorphism;



class MutableInt{
	int value = 1;
	public void increment () { ++value;      }
	public int  get ()       { return value; }
	List<String> gene = new ArrayList<String>();
	int geneCount = 0;

	public void incrementGene () { ++geneCount;      }
	public int getGeneCount() { return geneCount; }			

	public void addGene(String name){
		if(name != null && !gene.contains(name)){
			gene.add(name);
			incrementGene();
		}
	}

	public String getGene() { return Arrays.toString(gene.toArray()); }
	public String toString() { return (this.get()+ "\t"+ this.getGeneCount() + "\t"+ this.getGene()); }
}

public class EnrichAgent extends DBagent {
	private static final long serialVersionUID = 1L;
	
	/* Attributes */
	private String servicetype;
	private String infotype;
	private String fileName;
	private String summaryFile;
	private String enrichFile;
	private int analysistype;
	public String slash; // for Operating system control
	public String os; // for Operating system control (Linux,Windows)
	public String datapath;
	public String configpath;
	public String userdir;
	
	BufferedWriter efileWriter;

	/* Constructor */
	public EnrichAgent() {
		super();
		setServicetype("singleEnrichment");
		setInfoType("singleEnrichment");
		slash = System.getProperty("file.separator");
		os = System.getProperty("os.name");
		this.userdir = System.getProperty("user.dir");
		this.datapath = userdir+slash+"data"+slash;

	}
	
	/* Agent setup */
	protected void setup() {
		System.out.println("Agent " + getLocalName() + " started.");

		this.register();
		addBehaviour(new waitRequest());
	}
	
	/* Agent shutdown */
	
	protected void takeDown() {
		System.out.println("Agent" + getLocalName() + " shutdown.");

	}
	
	/* Agent register */
	protected void register() {
		/* Register with DF */
		try {
			DFAgentDescription dfd = new DFAgentDescription();
			dfd.setName(getAID());
			System.out.println(
					"Agent " + getLocalName() + " registering service type \"" + this.getServicetype() + "\" with DF");

			ServiceDescription sd = new ServiceDescription();
			sd.setName(this.getInfoType());
			sd.setType(this.getServicetype());
			dfd.addServices(sd);
			DFService.register(this, dfd);
		} catch (FIPAException fe) {
			fe.printStackTrace();
		}
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
				System.out.println("Agent " + getLocalName() +" received a REQUEST message from agent "+msg.getSender().getName());
				try {
					sender = msg.getSender();
					BioDataLite enrichData = (BioDataLite)msg.getContentObject();
					System.out.println("Agent " + getLocalName() +" received annotation path = "+ enrichData.getFullFilename() + " ...");
					addBehaviour(new SEAction(sender,enrichData));
				} catch (UnreadableException e) { e.printStackTrace(); }
			} else { block(); }
		}
	} 

	/* Agent action: access DB -> accessDBsnpLocal; send reply  */
	class SEAction extends FSMBehaviour {
		private static final long serialVersionUID = 1L;

		private static final String STATE_A = "A";
		private static final String STATE_B = "B";

		public SEAction(AID pa,BioDataLite bd){
			System.out.println("Agent " + getLocalName() +" executing request...");
			this.registerFirstState(new writeEnrichment(bd), STATE_A);
			this.registerLastState(new SendReply(pa), STATE_B);
			this.registerDefaultTransition(STATE_A,STATE_B);
		}

		public int onEnd() {
			System.out.println("Agent " + getLocalName() +" finished task.");
			return super.onEnd();
		}
	}
	
	/* Agent communication: send reply */
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
	
	/* Agent action: write summary and enrichment analysis */
	class writeEnrichment extends OneShotBehaviour {
		private static final long serialVersionUID = 1L;

		BioDataLite contentdata;
		BufferedReader inputStream;
		BufferedWriter fileWriter;
		
		String[] values;
		String[] values2;
		String annotationDataFile;
		int gfTagSize = 0;
		int transRegSize = 0;
		int functionSize = 0;
		int pgkbPathSize = 0;
		int pgkbDiseaseSize = 0;
		int pgkbDrugSize = 0;
		int omimDisorderSize = 0;
		int omimCytoLocSize = 0;
		int goBioPSize = 0;
		int goCelCSize = 0;
		int goMolFSize = 0;
		int reactomePathSize = 0;

		Map<String, MutableInt> geneNameCount = new HashMap<String, MutableInt>();
		Map<String, MutableInt> fxnCount = new HashMap<String, MutableInt>();
		Map<String, MutableInt> chrCount = new HashMap<String, MutableInt>();
		
		Map<String, MutableInt> functionCount = new HashMap<String, MutableInt>();
		Map<String, MutableInt> pathwayCount = new HashMap<String, MutableInt>();
		Map<String, MutableInt> drugCount = new HashMap<String, MutableInt>();
		Map<String, MutableInt> diseaseCount = new HashMap<String, MutableInt>();
		Map<String, MutableInt> goMolFunCount = new HashMap<String, MutableInt>();
		Map<String, MutableInt> goCelCompCount = new HashMap<String, MutableInt>();
		Map<String, MutableInt> goBioProCount = new HashMap<String, MutableInt>();
		Map<String, MutableInt> cytoLocCount = new HashMap<String, MutableInt>();
		Map<String, MutableInt> disorderCount = new HashMap<String, MutableInt>();
		Map<String, MutableInt> locTypeCount = new HashMap<String, MutableInt>();
		Map<String, MutableInt> gfTagCount = new HashMap<String, MutableInt>();
		Map<String, MutableInt> reactomePathCount = new HashMap<String, MutableInt>();


		public writeEnrichment(BioDataLite bd) {
			// TODO Auto-generated constructor stub
			contentdata = bd; 
			annotationDataFile = contentdata.getFullFilename();
			analysistype = contentdata.getAnalysistype();
			setSummaryFile(annotationDataFile+".enSum");
			setEnrichFile(annotationDataFile+".enrich");
			setAnalysistype(analysistype);

		}

		public void action(){

			int curLine = 0;
			try {
				
				System.out.println("Agent " + getLocalName() +" reading annotation at "+ datapath+annotationDataFile + " ...");
				
				inputStream = new BufferedReader(new FileReader(datapath+annotationDataFile));
				String l;
				while ((l = inputStream.readLine()) != null) {
				   if (curLine++ < 1) {
			            continue;
			        }
			
					values = l.split("\t");
					String gn = values[2];
					String tr = values[4];
					String ch = values[6];
					
					MutableInt count1 = geneNameCount.get(gn);
					if (count1 == null) {
					    geneNameCount.put(gn, new MutableInt());
					} else { count1.increment(); }

					MutableInt count2 = fxnCount.get(tr);
					if (count2 == null) {
					    fxnCount.put(tr, new MutableInt());
					} else { count2.increment(); }
					
					MutableInt count3 = chrCount.get(ch);
					if (count3 == null) {
					    chrCount.put(ch, new MutableInt());
					} else { count3.increment(); }
				
					if(getAnalysistype()==4 || getAnalysistype()==3){
						String fun = values[21];
						String rpath = values[22];
						String path = values[23];
						String drug = values[24];
						String dise = values[25];
						String gomf = values[27];
						String gocc = values[28];
						String gobp = values[29];
						String cyto = values[30];
						String diso = values[33];
						String locT = values[42];
						String gfTag = values[44];
		
						MutableInt count4 = functionCount.get(fun);
						if (count4 == null) {
						    functionCount.put(fun, new MutableInt());
						} else { count4.increment(); }
		
						values2 = path.split(";");
						for(String value : values2) {
							MutableInt count5 = pathwayCount.get(value);
							if (count5 == null) {
								pathwayCount.put(value, new MutableInt());
								pathwayCount.get(value).addGene(gn);
							} else {
								count5.increment();
								pathwayCount.get(value).addGene(gn);
							}
						}
		
						values2 = drug.split(";");
						for(String value : values2) {
							MutableInt count6 = drugCount.get(value);
							if (count6 == null) {
								drugCount.put(value, new MutableInt());
								drugCount.get(value).addGene(gn);
							} else {
							    count6.increment();
							    drugCount.get(value).addGene(gn);
							}
						}
		
						values2 = dise.split(";");
						for(String value : values2) {
							MutableInt count7 = diseaseCount.get(value);
							if (count7 == null) {
								diseaseCount.put(value, new MutableInt());
								diseaseCount.get(value).addGene(gn);
							} else {
							    count7.increment();
							    diseaseCount.get(value).addGene(gn);
							}
						}
		
						values2 = gomf.split(";");
						for(String value : values2) {
							MutableInt count8 = goMolFunCount.get(value);
							if (count8 == null) {
								goMolFunCount.put(value, new MutableInt());
								goMolFunCount.get(value).addGene(gn);
							} else {
							    count8.increment();
							    goMolFunCount.get(value).addGene(gn);
							}
						}
		
						values2 = gocc.split(";");
						for(String value : values2) {
							MutableInt count9 = goCelCompCount.get(value);
							if (count9 == null) {
								goCelCompCount.put(value, new MutableInt());
								goCelCompCount.get(value).addGene(gn);
							} else {
							    count9.increment();
							    goCelCompCount.get(value).addGene(gn);
							}
						}
		
						values2 = gobp.split(";");
						for(String value : values2) {
							MutableInt count10 = goBioProCount.get(value);
							if (count10 == null) {
								goBioProCount.put(value, new MutableInt());
								goBioProCount.get(value).addGene(gn);
							} else {
							    count10.increment();
							    goBioProCount.get(value).addGene(gn);
							}
						}
		
						MutableInt count11 = cytoLocCount.get(cyto);
						if (count11 == null) {
							cytoLocCount.put(cyto, new MutableInt());
							cytoLocCount.get(cyto).addGene(gn);
						} else {
						    count11.increment();
						    cytoLocCount.get(cyto).addGene(gn);
						}
							
						MutableInt count13 = locTypeCount.get(locT);
						if (count13 == null) {
							locTypeCount.put(locT, new MutableInt());
							locTypeCount.get(locT).addGene(gn);
						} else {
						    count13.increment();
						    locTypeCount.get(locT).addGene(gn);
						}
		
						MutableInt count15 = gfTagCount.get(gfTag);
						if (count15 == null) {
							gfTagCount.put(gfTag, new MutableInt());
							gfTagCount.get(gfTag).addGene(gn);
						} else {
						    count15.increment();
						    gfTagCount.get(gfTag).addGene(gn);
						}
		
						values2 = diso.split(";");
						for(String value : values2) {
							MutableInt count12 = disorderCount.get(value);
							if (count12 == null) {
								disorderCount.put(value, new MutableInt());
								disorderCount.get(value).addGene(gn);
							} else {
								count12.increment();
								disorderCount.get(value).addGene(gn);
							}
						}
						
						values2 = rpath.split(";");
						for(String value : values2) {
							MutableInt count16 = reactomePathCount.get(value);
							if (count16 == null) {
								reactomePathCount.put(value, new MutableInt());
								reactomePathCount.get(value).addGene(gn);
							} else {
								count16.increment();
								reactomePathCount.get(value).addGene(gn);
							}
						}
					}
				}
				inputStream.close();
			} catch (IOException e) { e.printStackTrace(); }

			try{

				System.out.println("Agent " + getLocalName() +" writing summary at "+ summaryFile + " ...");
				
				fileWriter = new BufferedWriter(new FileWriter(summaryFile));
				Iterator<Entry<String, MutableInt>> itr0 = geneNameCount.entrySet().iterator();
				while(itr0.hasNext()){
					Map.Entry<String, MutableInt> entry = itr0.next();
				    fileWriter.write("dbSNP Gene Symbol \t" +entry.getKey()+"\t"+entry.getValue().get()+"\tnull\n");	
				}

				Iterator<Entry<String, MutableInt>> itr2 = fxnCount.entrySet().iterator();
				while(itr2.hasNext()){
					Map.Entry<String, MutableInt> entry = itr2.next();
					fileWriter.write("dbSNP Transcript Region \t"+entry.getKey()+"\t"+entry.getValue().get()+"\tnull\n");
					transRegSize = transRegSize + Integer.valueOf(entry.getValue().get());
				}
				
				Iterator<Entry<String, MutableInt>> itr3 = chrCount.entrySet().iterator();
				while(itr3.hasNext()){
					Map.Entry<String, MutableInt> entry = itr3.next();
					fileWriter.write("dbSNP Chromosome \t"+entry.getKey()+"\t"+entry.getValue().get()+"\tnull\n");
				}

				if(getAnalysistype()==4 || getAnalysistype()==3){
					Iterator<Entry<String, MutableInt>> itr4 = functionCount.entrySet().iterator();
					while(itr4.hasNext()){
						Map.Entry<String, MutableInt> entry = itr4.next();
					    fileWriter.write("UCSC Transcript region \t"+entry.getKey()+"\t"+entry.getValue().get()+"\tnull\n");	
					}
					
					Iterator<Entry<String, MutableInt>> itr16 = reactomePathCount.entrySet().iterator();
					while(itr16.hasNext()){
						Map.Entry<String, MutableInt> entry = itr16.next();
					    fileWriter.write("Reactome Pathway\t"+entry.getKey()+"\t"+entry.getValue().get()+"\t"+entry.getValue().getGeneCount()+"\n");
					    reactomePathSize = reactomePathSize + Integer.valueOf(entry.getValue().getGeneCount());
					}

					Iterator<Entry<String, MutableInt>> itr5 = pathwayCount.entrySet().iterator();
					while(itr5.hasNext()){
						Map.Entry<String, MutableInt> entry = itr5.next();
					    fileWriter.write("PharmGKB Pathway\t"+entry.getKey()+"\t"+entry.getValue().get()+"\t"+entry.getValue().getGeneCount()+"\n");	
					    pgkbPathSize = pgkbPathSize + Integer.valueOf(entry.getValue().getGeneCount());
					}

					Iterator<Entry<String, MutableInt>> itr6 = drugCount.entrySet().iterator();
					while(itr6.hasNext()){
						Map.Entry<String, MutableInt> entry = itr6.next();
					    fileWriter.write("PharmGKB Drugs\t"+entry.getKey()+"\t"+entry.getValue().get()+"\t"+entry.getValue().getGeneCount()+"\n");
					    pgkbDrugSize = pgkbDrugSize + Integer.valueOf(entry.getValue().getGeneCount());
					}

					Iterator<Entry<String, MutableInt>> itr7 = diseaseCount.entrySet().iterator();
					while(itr7.hasNext()){
						Map.Entry<String, MutableInt> entry = itr7.next();
					    fileWriter.write("PharmGKB Disease\t"+entry.getKey()+"\t"+entry.getValue().get()+"\t"+entry.getValue().getGeneCount()+"\n");
					    pgkbDiseaseSize = pgkbDiseaseSize + Integer.valueOf(entry.getValue().getGeneCount());
					}

					Iterator<Entry<String, MutableInt>> itr8 = goMolFunCount.entrySet().iterator();
					while(itr8.hasNext()){
						Map.Entry<String, MutableInt> entry = itr8.next();
					    fileWriter.write("GO Molecular Function\t"+entry.getKey()+"\t"+entry.getValue().get()+"\t"+entry.getValue().getGeneCount()+"\n");
					    goMolFSize = goMolFSize + Integer.valueOf(entry.getValue().getGeneCount());
					}

					Iterator<Entry<String, MutableInt>> itr9 = goCelCompCount.entrySet().iterator();
					while(itr9.hasNext()){
						Map.Entry<String, MutableInt> entry = itr9.next();
					    fileWriter.write("GO Cellular Component\t"+entry.getKey()+"\t"+entry.getValue().get()+"\t"+entry.getValue().getGeneCount()+"\n");
					    goCelCSize = goCelCSize + Integer.valueOf(entry.getValue().getGeneCount());
					}

					Iterator<Entry<String, MutableInt>> itr10 = goBioProCount.entrySet().iterator();
					while(itr10.hasNext()){
						Map.Entry<String, MutableInt> entry = itr10.next();
					    fileWriter.write("GO Biological Process\t"+entry.getKey()+"\t"+entry.getValue().get()+"\t"+entry.getValue().getGeneCount()+"\n");
					    goBioPSize = goBioPSize + Integer.valueOf(entry.getValue().getGeneCount());
					}

					Iterator<Entry<String, MutableInt>> itr11 = cytoLocCount.entrySet().iterator();
					while(itr11.hasNext()){
						Map.Entry<String, MutableInt> entry = itr11.next();
					    fileWriter.write("OMIM CytoLocation\t"+entry.getKey()+"\t"+entry.getValue().get()+"\t"+entry.getValue().getGeneCount()+"\n");
					    omimCytoLocSize = omimCytoLocSize + Integer.valueOf(entry.getValue().getGeneCount());
					}

					Iterator<Entry<String, MutableInt>> itr12 = disorderCount.entrySet().iterator();
					while(itr12.hasNext()){
						Map.Entry<String, MutableInt> entry = itr12.next();
					    fileWriter.write("OMIM Disorders\t"+entry.getKey()+"\t"+entry.getValue().get()+"\t"+entry.getValue().getGeneCount()+"\n");
					    omimDisorderSize = omimDisorderSize + Integer.valueOf(entry.getValue().getGeneCount());
					}

					Iterator<Entry<String, MutableInt>> itr13 = locTypeCount.entrySet().iterator();
					while(itr13.hasNext()){
						Map.Entry<String, MutableInt> entry = itr13.next();
					    fileWriter.write("HGNC Locus Type \t"+entry.getKey()+"\t"+entry.getValue().get()+"\t"+entry.getValue().getGeneCount()+"\n");	
					}

					Iterator<Entry<String, MutableInt>> itr15 = gfTagCount.entrySet().iterator();
					while(itr15.hasNext()){
						Map.Entry<String, MutableInt> entry = itr15.next();
					    fileWriter.write("HGNC Gene Family \t"+entry.getKey()+"\t"+entry.getValue().get()+"\t"+entry.getValue().getGeneCount()+"\n");
					    gfTagSize = gfTagSize + Integer.valueOf(entry.getValue().getGeneCount());
					}

					System.out.println("ENRICHMENT TEST LOCAL");
					System.out.println("Agent " + getLocalName() +" writing enrichment analysis at "+ datapath+enrichFile + " ...");
					
					efileWriter = new BufferedWriter(new FileWriter(enrichFile));
					efileWriter.write("Database\tTerm\tp-Value\tGene Population Count\tSNP Count\tGene Sample Count\tGene List\n");
					
					BioEnrichment hgncCount = new BioEnrichment();
					hgncCount.setHgncGeneFamilyCount();
					System.out.println("Enrichment Test GeneFamily - Sample Matrix Size = "+ gfTagCount.size());
					EnrichmentTestRemote("HGNC Gene Family",gfTagCount,hgncCount.getHgncGeneFamilyCount(),gfTagSize);
					
					BioEnrichment edisorderCount = new BioEnrichment();

					edisorderCount.setomimMorbidMapCount1();
					edisorderCount.setomimMorbidMapCount2();
					System.out.println("Enrichment Test OmimDisorder - Sample Matrix Size = "+ disorderCount.size());
					EnrichmentTestRemote("OMIM MorbidMap Disorder",disorderCount,edisorderCount.getomimMorbidMapCount(),omimDisorderSize);
					
					BioEnrichment ecytoLocCount = new BioEnrichment();
					ecytoLocCount.setomimCytoLocCount();
					System.out.println("Enrichment Test CytoLoc - Sample Matrix Size = "+ disorderCount.size());
					EnrichmentTestRemote("OMIM CytoLoc",cytoLocCount,ecytoLocCount.getomimCytoLocCount(),omimCytoLocSize);

					BioEnrichment etransRegCount = new BioEnrichment();
					etransRegCount.setdbsnpTransRegCount();
					BioEnrichment egoMolFunCount = new BioEnrichment();
					egoMolFunCount.setgoMolFunctionCount();
					System.out.println("Enrichment Test GOMolFunction - Sample Matrix Size = "+ goMolFunCount.size());
					EnrichmentTestRemote("GO Molecular Function",goMolFunCount,egoMolFunCount.getgoMolFunctionCount(),goMolFSize);
					
					BioEnrichment egoBioPCount = new BioEnrichment();
					egoBioPCount.setgoBioProcessCount1();
					egoBioPCount.setgoBioProcessCount2();
					egoBioPCount.setgoBioProcessCount3();
					System.out.println("Enrichment Test GOBioProcess - Sample Matrix Size = "+ goBioProCount.size());
					EnrichmentTestRemote("GO Bio Process",goBioProCount,egoBioPCount.getgoBioProcessCount(),goBioPSize);

					BioEnrichment egoCelCompCount = new BioEnrichment();
					egoCelCompCount.setgoCelCompCount();
					System.out.println("Enrichment Test GOCelComp - Sample Matrix Size = "+ goCelCompCount.size());
					EnrichmentTestRemote("GO Cellular Component",goCelCompCount,egoCelCompCount.getgoCelCompCount(),goCelCSize);

					BioEnrichment erpathwayCount = new BioEnrichment();
					erpathwayCount.setreactomePathwayCount();
					System.out.println("Enrichment Test Reactome Pathway - Sample Matrix Size = "+ reactomePathCount.size());
					EnrichmentTestRemote("Reactome Pathways",reactomePathCount,erpathwayCount.getreactomePathwayCount(),reactomePathSize);
	
					BioEnrichment2 epathwayCount = new BioEnrichment2();
					epathwayCount.setpgkbPathwayCount();
					System.out.println("Enrichment Test PGKB Pathway - Sample Matrix Size = "+ pathwayCount.size());
					EnrichmentTestRemote("PharmGKB Pathways",pathwayCount,epathwayCount.getpgkbPathwayCount(),pgkbPathSize);
					
					BioEnrichment2 edrugCount = new BioEnrichment2();
					edrugCount.setpgkbDrugCount();
					System.out.println("Enrichment Test PGKB Drug - Sample Matrix Size = "+ drugCount.size());
					EnrichmentTestRemote("PharmGKB Drugs",drugCount,edrugCount.getpgkbDrugCount(),pgkbDrugSize);

					BioEnrichment2 ediseaseCount = new BioEnrichment2();
					ediseaseCount.setpgkbDiseaseCount();
					System.out.println("Enrichment Test PGKB Disease - Sample Matrix Size = "+ diseaseCount.size());
					EnrichmentTestRemote("PGKB Disease",diseaseCount,ediseaseCount.getpgkbDiseaseCount(),pgkbDiseaseSize);

					efileWriter.close();
				}
						
				fileWriter.close();
			} catch (IOException e) { e.printStackTrace(); }	
		}
	}

	
	
	
	
	public void EnrichmentTestRemote(String column, Map<String, MutableInt> miniMap, Map<String,String> completeMap, int sampleSize){
		Iterator<Entry<String, MutableInt>> itrE1 = miniMap.entrySet().iterator();
		while(itrE1.hasNext()){

			Iterator<Entry<String, String>> itrE2 = completeMap.entrySet().iterator();
			Map.Entry<String, MutableInt> entry1 = itrE1.next();
			while(itrE2.hasNext()){
				Map.Entry<String, String> entry2 = itrE2.next();
				if(entry1.getKey().equalsIgnoreCase(entry2.getKey()) && !entry1.getKey().contains("null")){
					String countCompleteMap = completeMap.get("COUNT");
					ContingencyTable2x2 Table = new ContingencyTable2x2(Integer.valueOf(entry1.getValue().getGeneCount()), sampleSize-Integer.valueOf(entry1.getValue().getGeneCount()), Integer.valueOf(entry2.getValue()), Integer.valueOf(countCompleteMap)-Integer.valueOf(entry2.getValue()));  
					FishersExactTest FisherTest = new FishersExactTest(Table);

					try {
						efileWriter.write(column+"\t"+entry1.getKey() +"\t"+FisherTest.getOneTailedSP() +"\t"+ entry2.getValue()+ "\t"+ entry1.getValue().toString()+"\n");
					} catch (IOException e) { e.printStackTrace(); }

					break;
				}
			}
		}
	}
	
	
	public void EnrichmentTestLocal(Map<String, String> miniMap, Map<String,String> completeMap, int sampleSize){
		Iterator<Entry<String, String>> itrE1 = miniMap.entrySet().iterator();
		while(itrE1.hasNext()){
			Iterator<Entry<String, String>> itrE2 = completeMap.entrySet().iterator();
			Map.Entry<String, String> entry1 = itrE1.next();
			while(itrE2.hasNext()){
				Map.Entry<String, String> entry2 = itrE2.next();
				if(entry2.getKey().equalsIgnoreCase(entry1.getKey())){
					String countCompleteMap = completeMap.get("COUNT");
					ContingencyTable2x2 Table = new ContingencyTable2x2(Integer.valueOf(entry1.getValue()), sampleSize-Integer.valueOf(entry1.getValue()), Integer.valueOf(entry2.getValue()), Integer.valueOf(countCompleteMap)-Integer.valueOf(entry2.getValue()));  
					FishersExactTest FisherTest = new FishersExactTest(Table);
					
					try {
						efileWriter.write("Term - "+ entry1.getKey() +"\tTailedP = " + FisherTest.getOneTailedSP()+"\n")	;
					} catch (IOException e) { e.printStackTrace(); }
					
					break;
				}
			}
		}
		
		try {
			efileWriter.write("\n");
		} catch (IOException e) { e.printStackTrace(); }
	}
	
	public void setSummaryFile(String sf){
		this.summaryFile    = this.datapath+sf;
	}
	
	public String getSummaryFile(){
		return this.summaryFile;
	}

	public void setEnrichFile(String ef){
		this.enrichFile    = this.datapath+ef;
	}
	
	public String getEnrichFile(){
		return this.enrichFile;
	}
	
	public String getServicetype() {
		return servicetype;
	}

	public void setServicetype(String s) {
		this.servicetype = s;
	}

	public String getInfoType() {
		return infotype;
	}

	public void setInfoType(String i) {
		this.infotype = i;
	}
	
	public int getAnalysistype() {
		return analysistype;
	}

	public void setAnalysistype(int analysistype) {
		this.analysistype = analysistype;
	}
}