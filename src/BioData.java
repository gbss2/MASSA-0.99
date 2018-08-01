import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

import BioData_Models.Gene;
import BioData_Models.GeneOntology;
import BioData_Models.HUGO;
import BioData_Models.OMIM;
import BioData_Models.PharmGKB;
import BioData_Models.Polymorphism;
import BioData_Models.UCSC;

public class BioData implements java.io.Serializable {
	private static final long serialVersionUID = 1L;

	private String[] snprslist;
	private int[]    snpidlist;
	private String[] genenamelist;
	private String[] geneidlist;
	private String genename;
	private int searchid;
	private int[] searchidlist;
	private int analysistype;
	
	public Polymorphism polymorphism;
	public Gene gene;
	public PharmGKB pharmgkb;
	public GeneOntology go;
	public OMIM omim;
	public UCSC ucsc;
	public HUGO hugo;
	 
	private List<PharmGKB> pharmlist;
	private List<Gene> genelist;
	private List<Polymorphism> snplist;
	private List<GeneOntology> golist;
	private List<OMIM> omimlist;
	private List<UCSC> ucsclist;
	private List<HUGO> hugolist;

	public Hashtable<String,String> snp_gene;

	public Hashtable<String,String> provenance_attrib;
	public Hashtable<String,String> provenance_version;
	public Hashtable<String,String> provenance_link;
	
	public BioData(){
		polymorphism = new Polymorphism();
		snplist = new ArrayList<Polymorphism>();
		
		pharmgkb = new PharmGKB();
		pharmlist = new ArrayList<PharmGKB>();
		
		go     = new GeneOntology();
		golist = new ArrayList<GeneOntology>();
		
		omim     = new OMIM();
		omimlist = new ArrayList<OMIM>();
		
		ucsc     = new UCSC();
		ucsclist = new ArrayList<UCSC>();
		
		hugo = new HUGO();
		hugolist = new ArrayList<HUGO>();
		
		provenance_attrib = new Hashtable<String,String>();
		provenance_version = new Hashtable<String,String>();
		snp_gene = new Hashtable<String,String>();
	}

	public BioData(String info){
		if(info.equals("snp")){
			polymorphism = new Polymorphism();
			snplist = new ArrayList<Polymorphism>();
		}
		if(info.equals("pharmgkb")){
			pharmgkb = new PharmGKB();
			pharmlist = new ArrayList<PharmGKB>();
		}
		if(info.equals("gene ontology")){
			go     = new GeneOntology();
			golist = new ArrayList<GeneOntology>();
		}

		if(info.equals("omim")){
			omim     = new OMIM();
			omimlist = new ArrayList<OMIM>();
		}
		
		if(info.equals("ucsc")){
			ucsc     = new UCSC();
			ucsclist = new ArrayList<UCSC>();
		}
		
		if(info.equals("hugo")){
			hugo     = new HUGO();
			hugolist = new ArrayList<HUGO>();
		}
		
		provenance_attrib = new Hashtable<String,String>();
		provenance_version = new Hashtable<String,String>();
		provenance_link = new Hashtable<String,String>();
		snp_gene = new Hashtable<String,String>();
	}
	
	public Hashtable<String,String> createSnpGeneAssoc(){
		Hashtable<String,String> snpgeneassoc =  new Hashtable<String,String>();

		for (Polymorphism p : this.getPolymorphismList()){
			if(!p.getGenesymbol().equals("null")){
				snpgeneassoc.put(p.getPolymorphismCode(),p.getGenesymbol());
			}	
		}
		
		return snpgeneassoc;
	}
	
	public void setSnpGene(Hashtable<String,String> sg){
		this.snp_gene = sg;
	}

	public Hashtable<String,String> getSnpGene(){
		return this.snp_gene;
	}
	
	public Polymorphism createPolymorphismInstance(){
		return new Polymorphism();
	}
	
	public Gene createGeneInstance(){
		return new Gene();
	}

	public PharmGKB createPharmGKBInstance(){
		return new PharmGKB();
	}

	public GeneOntology createGOInstance(){
		return new GeneOntology();
	}
	
	public OMIM createOMIMInstance(){
		return new OMIM();
	}

	public UCSC createUCSCInstance(){
		return new UCSC();
	}
	
	public HUGO createHUGOInstance(){
		return new HUGO();
	}

	public void setGeneList(Gene g){
		this.genelist.add(g);
	}

	public List<Gene> getGeneList(){
		return this.genelist;
	}
	
	public void setPolymorphismList(Polymorphism p){
		this.snplist.add(p);
	}

	public List<Polymorphism> getPolymorphismList(){
		return this.snplist;
	}

	public void setPharmgkbList(PharmGKB ph){
		this.pharmlist.add(ph);
	}

	public List<PharmGKB> getPharmgkbList(){
		return this.pharmlist;
	}
	
	public void setGOList(GeneOntology go){
		this.golist.add(go);
	}
	
	public List<GeneOntology> getGOList(){
		return this.golist;
	}
	
	public void setOMIMList(OMIM om){
		this.omimlist.add(om);
	}
	
	public List<OMIM> getOMIMList(){
		return this.omimlist;
	}

	public void setUCSCList(UCSC uc){
		this.ucsclist.add(uc);
	}
	
	public List<UCSC> getUCSCList(){
		return this.ucsclist;
	}
	
	public void setHUGOList(HUGO hu){
		this.hugolist.add(hu);
	}
	
	public List<HUGO> getHUGOList(){
		return this.hugolist;
	}
	
	public PharmGKB getPharmGKBObject(String polycode, List<PharmGKB> list){
		PharmGKB pharmobj = new PharmGKB();
		boolean flag = true;
		
		Iterator<PharmGKB> itr = list.iterator();
		while(itr.hasNext() && flag) {
			PharmGKB ph = (PharmGKB)itr.next();
			if(polycode.equals(ph.getPolymorphismCode())){
				pharmobj = ph;
				flag = false;
			}
		}	
		return pharmobj;
	}

	public GeneOntology getGOobject(String polycode, List<GeneOntology> list){
		GeneOntology goobj = new GeneOntology();
		boolean flag = true;
		
		Iterator<GeneOntology> itr = list.iterator();
		while(itr.hasNext() && flag) {
			GeneOntology go = (GeneOntology)itr.next();
			if(polycode.equals(go.getPolymorphismCode())){
				goobj = go;
			}
		}	
		return goobj;
	}

	public UCSC getUCSCobject(String polycode, List<UCSC> list){
		UCSC ucscobj = new UCSC();
		boolean flag = true;
		
		Iterator<UCSC> itr = list.iterator();
		while(itr.hasNext() && flag) {
			UCSC ucsc = (UCSC)itr.next();
			if(polycode.equals(ucsc.getPolymorphismCode())){
				ucscobj = ucsc;
			}
		}	
		return ucscobj;
	}

	public OMIM getOMIMobject(String polycode, List<OMIM> list){
		OMIM omimobj = new OMIM();
		boolean flag = true;
		
		Iterator<OMIM> itr = list.iterator();
		while(itr.hasNext() && flag) {
			OMIM omim = (OMIM)itr.next();
			if(polycode.equals(omim.getPolymorphismCode())){
				omimobj = omim;
			}
		}	
		return omimobj;
	}

	public HUGO getHUGOobject(String polycode, List<HUGO> list){
		HUGO hugoobj = new HUGO();
		boolean flag = true;
		
		Iterator<HUGO> itr = list.iterator();
		while(itr.hasNext() && flag) {
			HUGO hugo = (HUGO)itr.next();
			if(polycode.equals(hugo.getPolymorphismCode())){
				hugoobj = hugo;
			}
		}	
		return hugoobj;
	}

	public void setSnpRsList(String[] l){
		this.snprslist = new String[l.length];
		this.snprslist = l;
	}

	public void setSnpIdList(int[] l){
		this.snpidlist = new int[l.length];
		this.snpidlist = l;
	}
	
	public void setGeneNameList(String[] l){
		this.genenamelist = new String[l.length];
		this.genenamelist = l;
	}

	public void setGeneIdList(String[] l){
		this.geneidlist = new String[l.length];
		this.geneidlist = l;
	}

	public String[] getSnpRsList(){
		return snprslist;
	}
	
	public int[] getSnpIdList(){
		return snpidlist;
	}

	public String[] getGeneNameList(){
		return genenamelist;
	}

	public String[] getGeneIdList(){
		return geneidlist;
	}

	public String getGeneName() {
		return genename;
	}

	public void setGeneName(String genename) {
		this.genename = genename;
	}
	
	public String SnpGenetoString(Gene g){
		return 	"Gene Symbol = " +gene.getGeneSymbol()+
				"\nSNP id = "+gene.getSnpid(); 
	}   

	public int getAnalysistype() {
		return analysistype;
	}

	public void setAnalysistype(int analysistype) {
		this.analysistype = analysistype;
	}

	public int getSearchid() {
		return searchid;
	}

	public void setSearchid(int searchid) {
		this.searchid = searchid;
	}

	public int[] getSearchidlist() {
		return searchidlist;
	}

	public void setSearchidlist(int[] searchidlist) {
		this.searchidlist = searchidlist;
	}

	public Hashtable<String,String> getProvenance_attrib() {
		return provenance_attrib;
	}

	public void setProvenance_attrib(Hashtable<String,String> provenance_attrib) {
		this.provenance_attrib = provenance_attrib;
	}

	public Hashtable<String,String> getProvenance_version() {
		return provenance_version;
	}

	public void setProvenance_version(Hashtable<String,String> provenance_version) {
		this.provenance_version = provenance_version;
	}
	
	public Hashtable<String,String> getProvenance_link() {
		return provenance_link;
	}

	public void setProvenance_link(Hashtable<String,String> provenance_link) {
		this.provenance_link = provenance_link;
	}

	public String toString(){
		return "Gene Name = " + gene.getGeneName() +
				"\nGene Symbol = " +gene.getGeneSymbol()+
				"\nGene Alt Symbol = " +gene.getGeneAltSymbol()+ 
				"\nChromosome = " +polymorphism.getChromosome()+ 
				"\nPolymorphism Code = "+polymorphism.getPolymorphismCode()+ 
				"\nKind = " + polymorphism.getKind()+
				"\nSub-Kind = " +polymorphism.getSubKind();
	}

	public void setBasicProvenance(){
		provenance_attrib.put("Polymorphism Id","dbsnp");
		provenance_attrib.put("Polymorphism Type","dbsnp");
		provenance_attrib.put("Gene Symbol","dbsnp");
		provenance_attrib.put("Gene ID","dbsnp");
		provenance_attrib.put("Transcript Region","dbsnp");
		provenance_attrib.put("Nucleotide Numbering coding DNA","dbsnp");
		provenance_attrib.put("Chromosome","dbsnp");
		provenance_attrib.put("Chromosome Position","dbsnp");
		provenance_attrib.put("Ancestral Allele","dbsnp");
		provenance_attrib.put("Orientation","dbsnp");
		provenance_attrib.put("Assembly Build Version","dbsnp");
		provenance_attrib.put("Assembly Coord Start","dbsnp");
		provenance_attrib.put("Assembly Coord End","dbsnp");
		provenance_attrib.put("mRNA accession","dbsnp");
		provenance_attrib.put("mRNA version","dbsnp");
		provenance_attrib.put("Alleles","dbsnp");
		provenance_attrib.put("Frequency","dbsnp");

		provenance_version.put("dbsnp","online");
	}

	public void setCompleteProvenance(){
		provenance_attrib.put("Polymorphism Id","dbsnp");
		provenance_attrib.put("Polymorphism Type","dbsnp");
		provenance_attrib.put("Gene Symbol","dbsnp");
		provenance_attrib.put("Gene ID","dbsnp");
		provenance_attrib.put("Transcript Region","dbsnp");
		provenance_attrib.put("Nucleotide Numbering coding DNA","dbsnp");
		provenance_attrib.put("Chromosome","dbsnp");
		provenance_attrib.put("Chromosome Position","dbsnp");
		provenance_attrib.put("Ancestral Allele","dbsnp");
		provenance_attrib.put("Orientation","dbsnp");
		provenance_attrib.put("Assembly Build Version","dbsnp");
		provenance_attrib.put("Assembly Coord Start","dbsnp");
		provenance_attrib.put("Assembly Coord End","dbsnp");
		provenance_attrib.put("mRNA accession","dbsnp");
		provenance_attrib.put("mRNA version","dbsnp");
		provenance_attrib.put("Alleles","dbsnp");
		provenance_attrib.put("Frequency","dbsnp");

		provenance_attrib.put("Strand","ucsc");	
		provenance_attrib.put("RefUCSC","ucsc");	
		provenance_attrib.put("ObservedUCSC","ucsc");	
		provenance_attrib.put("Polymorphism Class","ucsc");	
		provenance_attrib.put("Functional Class","ucsc");	

		provenance_attrib.put("Pathway list","pharmgkb");
		provenance_attrib.put("Drug list","pharmgkb");
		provenance_attrib.put("Disease list","pharmgkb");
		provenance_attrib.put("Genex list","pharmgkb");

		provenance_attrib.put("GO Molecular Function","gene ontology");
		provenance_attrib.put("GO Cellular Component","gene ontology");
		provenance_attrib.put("GO Biological Process","gene ontology");

		provenance_attrib.put("Cytoloc","omim");
		provenance_attrib.put("Gene Status","omim");
		provenance_attrib.put("Gene Map Methods","omim");
		provenance_attrib.put("Disorders","omim");
		provenance_attrib.put("MIM ids","omim");
		provenance_attrib.put("Inheritance","omim");
		provenance_attrib.put("Pheno Map Methods","omim");
		provenance_attrib.put("Comments","omim");

		provenance_attrib.put("HgncId","hgnc");
		provenance_attrib.put("GeneName","hgnc");
		provenance_attrib.put("GeneSynonyms","hgnc");
		provenance_attrib.put("LocusType","hgnc");
		provenance_attrib.put("LocusGroup","hgnc");
		provenance_attrib.put("GeneFamilyTag","hgnc");
		provenance_attrib.put("GeneFamily","hgnc");
		provenance_attrib.put("SpecialistDBLinks","hgnc");
		provenance_attrib.put("LocusSpecDB","hgnc");
		provenance_attrib.put("EnzymeId","hgnc");
		provenance_attrib.put("EntrezId","hgnc");
		provenance_attrib.put("EnsemblId","hgnc");
		provenance_attrib.put("PubMedIds","hgnc");
		provenance_attrib.put("RefSeqIds","hgnc");
		provenance_attrib.put("CCDSIds","hgnc");
		provenance_attrib.put("VegaIds","hgnc");
		provenance_attrib.put("UniProtId","hgnc");
		provenance_attrib.put("MouseGdbId","hgnc");
		provenance_attrib.put("RatGdbId","hgnc");
		
		provenance_version.put("dbsnp","online");
		provenance_version.put("pharmgkb","online");
		provenance_version.put("ucsc","online");
		provenance_version.put("gene ontology","online");
		provenance_version.put("omim","online");
		provenance_version.put("hgnc","online");
	}	

	public void setDataProvenanceLink(){
		provenance_link.put("dbsnp","ftp://ftp.ncbi.nih.gov/snp/00readme.txt");
		provenance_link.put("pharmGKB","http://www.pharmgkb.org/page/faqs");
		provenance_link.put("GO","http://www.geneontology.org/GO.database.schema.shtml");
		provenance_link.put("OMIM","http://omim.org/help/search");
		provenance_link.put("HGNC","http://www.genenames.org/data/gdlw_columndef.html#curatedfielddefinitions");
		provenance_link.put("ucsc","http://genome.ucsc.edu/goldenPath/help/hgTracksHelp.html, http://hgdownload.cse.ucsc.edu/downloads.html#human");
		provenance_link.put("ucsc ","http://genome.ucsc.edu/goldenPath/gbdDescriptionsOld.html");
		provenance_link.put("ucsc","http://genome.ucsc.edu/cgi-bin/hgTrackUi?db=hg19&g=cons46way");
	}
}