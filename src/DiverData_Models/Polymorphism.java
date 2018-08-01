package DiverData_Models;

public class Polymorphism implements java.io.Serializable{
	private static final long serialVersionUID = 1L;

	private String polymorphismCode;
	private String kind;
	private String subKind;
	private String referenceValue;
	private long coordRelGene;
	private String chromosome;
	private long coordRefSeq;
	private String assm_build_version;
	private String assm_coord_start;
	private String assm_coord_end;

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


	public long getCoordRelGene() {
		return coordRelGene;
	}

	public void setCoordRelGene(long coordRelGene) {
		this.coordRelGene = coordRelGene;
	}

	public String getChromosome() {
		return chromosome;
	}

	public void setChromosome(String chromosome) {
		this.chromosome = chromosome;
	}

	public long getCoordRefSeq() {
		return coordRefSeq;
	}

	public void setCoordRefSeq(long coordRefSeq) {
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
}