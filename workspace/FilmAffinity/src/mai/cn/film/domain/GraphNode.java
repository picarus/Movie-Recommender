package mai.cn.film.domain;

public interface GraphNode {
	
	void setIdxNode(int idx); // to assign the node idx
	int getIdxNode();
	String getLabel();
	Type getType();
	
}
