package BioData_Models;

import java.util.ArrayList;
import java.util.List;

public class PharmGKB implements java.io.Serializable{
	private static final long serialVersionUID = 1L;

	private List<String> drug;
	private List<String> pathway;
	private List<String> pathwayType;
	private List<String> haplotype;
	private List<String> genex;
	private List<String> disease;
	private List<String> literature;
	private List<String> polymorphismIds;
	private List<String> polymorphismLoc;
	private String polymorphismCode;
	private String genesymbol;
	private String genePgkbId;
			
	public PharmGKB(){
		drug = new ArrayList<String>();
		pathway = new ArrayList<String>();
		pathwayType = new ArrayList<String>();
		haplotype = new ArrayList<String>();
		genex = new ArrayList<String>();
		disease = new ArrayList<String>();
		literature = new ArrayList<String>();
		polymorphismIds = new ArrayList<String>();
		polymorphismLoc = new ArrayList<String>();
	}
	
	public String getGenesymbol() {
		return genesymbol;
	}

	public void setGenesymbol(String genesymbol) {
		this.genesymbol = genesymbol;
	}
	
	public void setDrugItem(String d){
		this.drug.add(d);
	}

	public List<String> getDrugList(){
		return this.drug;
	}
	
	public List<String> getDrug() {
		return drug;
	}

	public void setDrug(List<String> drug) {
		this.drug = drug;
	}
	
	public void setDiseaseItem(String d){
		this.disease.add(d);
	}

	public List<String> getDiseaseList(){
		return this.disease;
	}
	
	public List<String> getDisease() {
		return disease;
	}

	public void setDisease(List<String> disease) {
		this.disease = disease;
	}

	public void setPathwayItem(String d){
		this.pathway.add(d);
	}

	public List<String> getPathwayList(){
		return this.pathway;
	}
	
	public List<String> getPathway() {
		return pathway;
	}

	public void setPathway(List<String> pathway) {
		this.pathway = pathway;
	}

	public void setGenexItem(String d){
		this.genex.add(d);
	}

	public List<String> getGenexList(){
		return this.genex;
	}

	public List<String> getGenex() {
		return genex;
	}

	public void setGenex(List<String> genex) {
		this.genex = genex;
	}
	
	public List<String> getPathwayType() {
		return pathwayType;
	}

	public List<String> getPathwayTypeList(){
		return this.pathwayType;
	}
	
	public void setPathwayType(List<String> pathwayType) {
		this.pathwayType = pathwayType;
	}
	
	public void setPathwayTypeItem(String d) {
		this.pathwayType.add(d);
	}
	
	public List<String> getHaplotype() {
		return haplotype;
	}

	public void setHaplotype(List<String> haplotype) {
		this.haplotype = haplotype;
	}

	public void setHaplotypeItem(String d) {
		this.haplotype.add(d);
	}

	public List<String> getLiterature() {
		return literature;
	}

	public void setLiterature(List<String> literature) {
		this.literature = literature;
	}

	public void setLiteratureItem(String d) {
		this.literature.add(d);
	}
	
	public List<String> getPolymorphismLoc() {
		return polymorphismLoc;
	}

	public void setPolymorphismLoc(List<String> polymorphismLoc) {
		this.polymorphismLoc = polymorphismLoc;
	}

	public void setPolymorphismLocItem(String d) {
		this.polymorphismLoc.add(d);
	}

	public void setPolymorphismCode(String polymorphismCode) {
		this.polymorphismCode = polymorphismCode;
	}

	public List<String> getPolymorphismIds() {
		return polymorphismIds;
	}

	public void setPolymorphismIds(List<String> polymorphismIds) {
		this.polymorphismIds = polymorphismIds;
	}
	
	public void setPolymorphismIdsItem(String d) {
		this.polymorphismIds.add(d);
	}

	public String getPolymorphismCode() {
		return polymorphismCode;
	}

	public String getGenePgkbId() {
		return genePgkbId;
	}

	public void setGenePgkbId(String genePgkbId) {
		this.genePgkbId = genePgkbId;
	}
}