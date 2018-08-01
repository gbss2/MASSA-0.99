import DiverData_Models.Gene;
import DiverData_Models.Genotype;
import DiverData_Models.Haplotype;
import DiverData_Models.Person;
import DiverData_Models.Phenotype;
import DiverData_Models.Polymorphism;
import DiverData_Models.Population;
import DiverData_Models.ReferenceSequence;
import DiverData_Models.Sample;

public class DiverData implements java.io.Serializable {
	private static final long serialVersionUID = 1L;

	private String[] snprslist;
	private int[]    snpidlist;
	private String[] genenamelist;
	private String[] geneidlist;
	private String genename;
	public Protein protein;
	public Polymorphism polymorphism;
	public Genotype genotype;
	public Gene gene;
	public Haplotype haplotype;
	public ReferenceSequence referenceSequence;
	public Sample sample;
	public Person person;
	public Population population;
	public Phenotype phenotype;
		
	public DiverData(){
		protein = new Protein();
		polymorphism = new Polymorphism();
		genotype = new Genotype();
		gene = new Gene();
		haplotype = new Haplotype();
		referenceSequence = new ReferenceSequence();
		sample = new Sample();
		person = new Person();
		population = new Population();
		phenotype = new Phenotype();
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

	public class Protein implements java.io.Serializable{
		private static final long serialVersionUID = 1L;

		private String sequence;
		
		public String getSequence() {
			return sequence;
		}

		public void setSequence(String sequence) {
			this.sequence = sequence;
		}	
	}

	public String toString(){
		return "Gene Name = " + gene.getGeneName() + 
				"\nGene Alt Symbol = " +gene.getGeneAltSymbol()+ 
				"\nChromosome = " +polymorphism.getChromosome()+ 
				"\nPolymorphism Code = "+polymorphism.getPolymorphismCode()+ 
				"\nKind = " + polymorphism.getKind()+
				"\nSub-Kind = " +polymorphism.getSubKind()+ 
				"\nHaplotype ID = " +haplotype.getHaplotypeID()+ 
				"\nHaplotype Value = "+haplotype.getHaplotypeValue()+ 
				"\nGenotype Value = " + genotype.getGenotypeValue()+
				"\nReference Sequence = " + referenceSequence.getReferenceSequenceID()+ 
				"\nSample Code = " +sample.getSampleCode()+ 
				"\nFamily Id = "+person.getFamilyID()+ 
				"\nPerson Code = " + person.getPersonCode()+
				"\nPopulation Name = " + population.getPopulationName()+ 
				"\nPhenotype = " +phenotype.getPhenotype()+ 
				"\nAffected Status = "+phenotype.isAffStatus();  
	 }   
}