package BioData_Models;

import java.util.ArrayList;
import java.util.List;

public class Polymorphism implements java.io.Serializable{
	private static final long serialVersionUID = 1L;

	private String polymorphismCode;
	private String kind;
	private String subKind;
	private String referenceValue;
	private String coordRelGene;
	private String chromosome;
	private String coordRefSeq;
	private String assm_build_version;
	private String assm_coord_start;
	private String assm_coord_end;
	private String genesymbol;
	private String geneid;
	private String value;
	private String ancestralAllele;
	private String orientation;
	private String mrnaAcc;
	private String mrnaVer;
	private String freq;
	private String referenceAllele;		

	private List<String> drug;
	private List<String> pathway;
	private List<String> genex;
	private List<String> disease;

 	private List<String> molFunction;
	private List<String> celComponent;
	private List<String> bioProcess;

	private String strand;
	private String refUCSC;
	private String obsGen;
	private String ucscClass;
	private String ucscFunc;

	private String cytoloc;
	private String genestatus;
	private String genemapmethods;
	private List<String> disorder;
	private List<String> mimid;
	private List<String> inheritance;
	private List<String> phenomapmethods;
	private List<String> comments;
	private List<String> references;

	private String hgncId;
	private String hggeneName;
	private String geneSynonyms;
	private String locusType;
	private String locusGroup;
	private String geneFamilyTag;
	private String geneFamily;
	private String specialistDBLinks;
	private String locusSpecDB;
	private String enzymeId;
	private String entrezId;
	private String ensemblId;
	private String pubMedIds;
	private String RefSeqIds;
	private String CCDSIds;
	private String vegaIds;
	private String omimId;
	private String uniProtId;
	private String ucscId;
	private String mouseGdbId;
	private String ratGdbId;
	
	public Polymorphism(){
		drug    = new ArrayList<String>();
		pathway = new ArrayList<String>();
		genex   = new ArrayList<String>();
		disease = new ArrayList<String>();
		molFunction = new ArrayList<String>();
		celComponent= new ArrayList<String>();
		bioProcess  = new ArrayList<String>();
	}
	
	public String getPolymorphismCode() {
		return polymorphismCode;
	}

	public void setPolymorphismCode(String polymorphismCode) {
		this.polymorphismCode = polymorphismCode;
	}

	public String getKind() {
		return kind;
	}

	public void setKind(String kind) {
		this.kind = kind;
	}

	public String getSubKind() {
		return subKind;
	}

	public void setSubKind(String subKind) {
		this.subKind = subKind;
	}

	public String getReferenceValue() {
		return referenceValue;
	}

	public void setReferenceValue(String referenceValue) {
		this.referenceValue = referenceValue;
	}

	public String getCoordRelGene() {
		return coordRelGene;
	}

	public void setCoordRelGene(String coordRelGene) {
		this.coordRelGene = coordRelGene;
	}

	public String getChromosome() {
		return chromosome;
	}

	public void setChromosome(String chromosome) {
		this.chromosome = chromosome;
	}

	public String getCoordRefSeq() {
		return coordRefSeq;
	}

	public void setCoordRefSeq(String coordRefSeq) {
		this.coordRefSeq = coordRefSeq;
	}

	public String getAssm_build_version() {
		return assm_build_version;
	}

	public void setAssm_build_version(String assm_build_version) {
		this.assm_build_version = assm_build_version;
	}

	public String getAssm_coord_start() {
		return assm_coord_start;
	}

	public void setAssm_coord_start(String assm_coord_start) {
		this.assm_coord_start = assm_coord_start;
	}

	public String getAssm_coord_end() {
		return assm_coord_end;
	}

	public void setAssm_coord_end(String assm_coord_end) {
		this.assm_coord_end = assm_coord_end;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public String getAncestralAllele() {
		return ancestralAllele;
	}

	public void setAncestralAllele(String ancestralAllele) {
		this.ancestralAllele = ancestralAllele;
	}

	public String getOrientation() {
		return orientation;
	}

	public void setOrientation(String orientation) {
		this.orientation = orientation;
	}

	public String getMrnaAcc() {
		return mrnaAcc;
	}

	public void setMrnaAcc(String mrnaAcc) {
		this.mrnaAcc = mrnaAcc;
	}

	public String getMrnaVer() {
		return mrnaVer;
	}

	public void setMrnaVer(String mrnaVer) {
		this.mrnaVer = mrnaVer;
	}

	public String getFreq() {
		return freq;
	}

	public void setFreq(String freq) {
		this.freq = freq;
	}

	public String getReferenceAllele() {
		return referenceAllele;
	}

	public void setReferenceAllele(String referenceAllele) {
		this.referenceAllele = referenceAllele;
	}

	public String getGenesymbol() {
		return genesymbol;
	}

	public void setGenesymbol(String genesymbol) {
		this.genesymbol = genesymbol;
	}

	public String getGeneid() {
		return geneid;
	}

	public void setGeneid(String geneid) {
		this.geneid = geneid;
	}

	public void setDrugItem(String d){
		this.drug.add(d);
	}

	public void setDrugList(List<String> list){
		this.drug = list;
	}
	
	public List<String> getDrugList(){
		return this.drug;
	}

	public void setDiseaseItem(String d){
		this.disease.add(d);
	}

	public void setDiseaseList(List<String> list){
		this.disease = list;
	}

	public List<String> getDiseaseList(){
		return this.disease;
	}

	public void setPathwayItem(String d){
		this.pathway.add(d);
	}

	public void setPathwayList(List<String> list){
		this.pathway = list;
	}
	
	public List<String> getPathwayList(){
		return this.pathway;
	}

	public void setGenexItem(String d){
		this.genex.add(d);
	}

	public void setGenexList(List<String> list){
		this.genex = list;
	}
	
	public List<String> getGenexList(){
		return this.genex;
	}

	public List<String> getMolFunction() {
		return this.molFunction;
	}

	public void setMolFunctionItem(String i) {
		this.molFunction.add(i);
	}
	
	public void setMolFunctionList(List<String> l){
		this.molFunction = l;
	}

	public List<String> getCelComponent() {
		return this.celComponent;
	}

	public void setCelComponentItem(String i) {
		this.celComponent.add(i);
	}

	public void setCelComponentList(List<String> l){
		this.celComponent = l;
	}

	public List<String> getBioProcess() {
		return this.bioProcess;
	}

	public void setBioProcessItem(String i) {
		this.bioProcess.add(i);
	}

	public void setBioProcessList(List<String> l){
		this.bioProcess = l;
	}

	public String getStrand() {
		return strand;
	}

	public void setStrand(String strand) {
		this.strand = strand;
	}

	public String getRefUCSC() {
		return refUCSC;
	}

	public void setRefUCSC(String refUCSC) {
		this.refUCSC = refUCSC;
	}

	public String getObsGen() {
		return obsGen;
	}

	public void setObsGen(String obsGen) {
		this.obsGen = obsGen;
	}

	public String getUcscClass() {
		return ucscClass;
	}

	public void setUcscClass(String ucscClass) {
		this.ucscClass = ucscClass;
	}

	public String getUcscFunc() {
		return ucscFunc;
	}

	public void setUcscFunc(String ucscFunc) {
		this.ucscFunc = ucscFunc;
	}

	public String getCytoloc() {
		return cytoloc;
	}

	public void setCytoloc(String cytoloc) {
		this.cytoloc = cytoloc;
	}

	public String getGenestatus() {
		return genestatus;
	}

	public void setGenestatus(String genestatus) {
		this.genestatus = genestatus;
	}

	public String getGenemapmethods() {
		return genemapmethods;
	}

	public void setGenemapmethods(String genemapmethods) {
		this.genemapmethods = genemapmethods;
	}
	
	public List<String> getDisorder() {
		return disorder;
	}

	public void setDisorderItem(String i) {
		this.disorder.add(i);
	}
	
	public void setDisorderList(List<String> l){
		this.disorder = l;
	}

	public List<String> getComments() {
		return comments;
	}

	public void setCommentsItem(String i) {
		this.comments.add(i);
	}

	public void setCommentsList(List<String> l){
		this.comments = l;
	}

	public List<String> getReferences() {
		return references;
	}

	public void setReferencesItem(String i) {
		this.references.add(i);
	}
	public void setReferencesList(List<String> l){
		this.references = l;
	}

	public List<String> getMimID() {
		return mimid;
	}

	public void setMimIDItem(String i) {
		this.mimid.add(i);
	}
	public void setMimIDList(List<String> l){
		this.mimid = l;
	}
	
	public List<String> getInheritance() {
		return inheritance;
	}

	public void setInheritanceItem(String i) {
		this.inheritance.add(i);
	}
	public void setInheritanceList(List<String> l){
		this.inheritance = l;
	}

	public List<String> getPhenoMapMethods() {
		return phenomapmethods;
	}

	public void setPhenoMapMethodsItem(String i) {
		this.phenomapmethods.add(i);
	}
	public void setPhenoMapMethodsList(List<String> l){
		this.phenomapmethods = l;
	}

	public String getHgncId() {
		return hgncId;
	}

	public void setHgncId(String hgncId) {
		this.hgncId = hgncId;
	}

	public String getHgGeneName() {
		return hggeneName;
	}

	public void setHgGeneName(String geneName) {
		this.hggeneName = geneName;
	}

	public String getGeneSynonyms() {
		return geneSynonyms;
	}

	public void setGeneSynonyms(String geneSynonyms) {
		this.geneSynonyms = geneSynonyms;
	}

	public String getLocusType() {
		return locusType;
	}

	public void setLocusType(String locusType) {
		this.locusType = locusType;
	}

	public String getLocusGroup() {
		return locusGroup;
	}

	public void setLocusGroup(String locusGroup) {
		this.locusGroup = locusGroup;
	}

	public String getGeneFamilyTag() {
		return geneFamilyTag;
	}

	public void setGeneFamilyTag(String geneFamilyTag) {
		this.geneFamilyTag = geneFamilyTag;
	}

	public String getGeneFamily() {
		return geneFamily;
	}

	public void setGeneFamily(String geneFamily) {
		this.geneFamily = geneFamily;
	}

	public String getSpecialistDBLinks() {
		return specialistDBLinks;
	}

	public void setSpecialistDBLinks(String specialistDBLinks) {
		this.specialistDBLinks = specialistDBLinks;
	}

	public String getLocusSpecDB() {
		return locusSpecDB;
	}

	public void setLocusSpecDB(String locusSpecDB) {
		this.locusSpecDB = locusSpecDB;
	}

	public String getEnzymeId() {
		return enzymeId;
	}

	public void setEnzymeId(String enzymeId) {
		this.enzymeId = enzymeId;
	}

	public String getEntrezId() {
		return entrezId;
	}

	public void setEntrezId(String entrezId) {
		this.entrezId = entrezId;
	}

	public String getEnsemblId() {
		return ensemblId;
	}

	public void setEnsemblId(String ensemblId) {
		this.ensemblId = ensemblId;
	}

	public String getPubMedIds() {
		return pubMedIds;
	}

	public void setPubMedIds(String pubMedIds) {
		this.pubMedIds = pubMedIds;
	}

	public String getRefSeqIds() {
		return RefSeqIds;
	}

	public void setRefSeqIds(String refSeqIds) {
		RefSeqIds = refSeqIds;
	}

	public String getCCDSIds() {
		return CCDSIds;
	}

	public void setCCDSIds(String cCDSIds) {
		CCDSIds = cCDSIds;
	}

	public String getVegaIds() {
		return vegaIds;
	}

	public void setVegaIds(String vegaIds) {
		this.vegaIds = vegaIds;
	}

	public String getUniProtId() {
		return uniProtId;
	}

	public void setUniProtId(String uniProtId) {
		this.uniProtId = uniProtId;
	}

	public String getOmimId() {
		return omimId;
	}

	public void setOmimId(String omimId) {
		this.omimId = omimId;
	}

	public String getMouseGdbId() {
		return mouseGdbId;
	}

	public void setMouseGdbId(String mouseGdbId) {
		this.mouseGdbId = mouseGdbId;
	}

	public String getUcscId() {
		return ucscId;
	}

	public void setUcscId(String ucscId) {
		this.ucscId = ucscId;
	}

	public String getRatGdbId() {
		return ratGdbId;
	}

	public void setRatGdbId(String ratGdbId) {
		this.ratGdbId = ratGdbId;
	}
}