import java.util.Hashtable;
import java.util.List;
import java.util.ArrayList;

public class BioDataLite implements java.io.Serializable {
	private static final long serialVersionUID = 1L;

	private String[] snprslist;
	private int[]    snpidlist;
	private String[] genenamelist;
	private String[] geneidlist;
	private String genename;
	private int searchid;
	private List<Integer> searchidlist;
	private int analysistype;
	private int service_type;
	private String filename_path;
	private String genomeVersion;
	private String species;
	private String inputFormat;
	public Hashtable<String,String> provenance_attrib;
	public Hashtable<String,String> provenance_version;
	
	public BioDataLite(){
		searchidlist =  new ArrayList<Integer>();
		provenance_attrib = new Hashtable<String,String>();
		provenance_version = new Hashtable<String,String>();		
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

		provenance_version.put("dbsnp","150");
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
		provenance_version.put("dbsnp","137");

		provenance_attrib.put("Strand","ucsc");
		provenance_attrib.put("UCSC Reference","ucsc");
		provenance_attrib.put("UCSC Observed","ucsc");
		provenance_attrib.put("UCSC Polymorphism Class","ucsc");
		provenance_attrib.put("UCSC Functional Class","ucsc");
		provenance_version.put("ucsc","x");

		provenance_attrib.put("Pathway","pharmgkb");
		provenance_attrib.put("Disease","pharmgkb");
		provenance_attrib.put("Drug","pharmgkb");
		provenance_attrib.put("Related Genes","pharmgkb");
		provenance_version.put("pharmgkb","x");
		
		provenance_attrib.put("Biological Process","gene ontology");
		provenance_attrib.put("Molecular Function","gene ontology");
		provenance_attrib.put("Celular Component","gene ontology");
		provenance_version.put("gene ontology","x");

		provenance_attrib.put("Disorder","omim");
		provenance_attrib.put("Comments","omim");
		provenance_attrib.put("CytoLoc","omim");
		provenance_version.put("omim","x");
		
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

		provenance_attrib.put("pubmedid","gwas_catalog");
		provenance_attrib.put("reportedgenes","gwas_catalog");
		provenance_attrib.put("riskallele","gwas_catalog");
		provenance_attrib.put("context","gwas_catalog");
		provenance_attrib.put("pvalue","gwas_catalog");

		provenance_attrib.put("polyphenScore","polyphen");

		provenance_attrib.put("siftScore","sift");

		provenance_attrib.put("proveanScore","provean");

		provenance_attrib.put("reactomePathway","reactome");
		
		provenance_version.put("dbsnp","150_20-03-2018");
		provenance_version.put("pharmgkb","05-04-2018");
		provenance_version.put("ucsc","hg19_150_20-03-2018");
		provenance_version.put("gene ontology","21-03-2018");
		provenance_version.put("omim","09-04-2018");
		provenance_version.put("hgnc","26-03-2018");
		provenance_version.put("gwas_catalog","1.0.1_05-04-2018");
		provenance_version.put("provean","12-16-2013");
		provenance_version.put("sift","12-16-2013");
		provenance_version.put("polyphen","12-16-2013");
		provenance_version.put("reactome","63_21-03-2018");
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

	public int getSearchid() {
		return searchid;
	}

	public void setSearchid(int searchid) {
		this.searchid = searchid;
	}

	public List<Integer> getSearchidlist() {
		return searchidlist;
	}

	public void setSearchidlist(List<Integer> searchidlist) {
		this.searchidlist = searchidlist;
	}

	public void setSearchidItem(int item){
		this.searchidlist.add(item);
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
		return filename_path;
	}
	
	public void setFullFilename(String filename_path) {
		this.filename_path = filename_path;
	}
	
	public String getGenomeVersion() {
		return genomeVersion;
	}
	
	public void setGenomeVersion(String genomeVersion) {
		this.genomeVersion = genomeVersion;
	}
	
	public String getSpecies() {
		return species;
	}
	
	public void setSpecies(String species) {
		this.species = species;
	}
	
	public String getInputFormat() {
		return inputFormat;
	}
	
	public void setInputFormat(String inputFormat) {
		this.inputFormat = inputFormat;
	}
	
}