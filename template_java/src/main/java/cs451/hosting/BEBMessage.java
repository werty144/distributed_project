package cs451.hosting;

public class BEBMessage {
    public Integer SenderID;
    public String content;

    public BEBMessage(Integer SenferID, String content) {
        this.content = content;
        this.SenderID = SenferID;
    }
    @Override
    public int hashCode() {
        return (SenderID.toString() + '$' + content).hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (getClass() != obj.getClass()) {
            return false;
        }
        BEBMessage other = (BEBMessage) obj;
        return other.content.equals(content) && other.SenderID.equals(SenderID);
    }
}
