public class SampleDBagent extends DBagent {
	private static final long serialVersionUID = 1L;

    public SampleDBagent() {
    	this.setDBname("sampledb");
   	 	this.setInformation("sample"); 
    }

    protected void setup() {
        System.out.println("Agent "+getLocalName()+" started.");
        this.register();
    }
}