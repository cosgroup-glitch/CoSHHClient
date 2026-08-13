package integrations.mapv4;

public class GridInfo
{
    public long gridId;
    public int x;
    public int y;
    
    public GridInfo(long gridId, int x, int y) {
	this.gridId = gridId;
	this.x = x;
	this.y = y;
    }
    
    public long getGridId() {
	return gridId;
    }
    public int getX() {
	return x;
    }
    public int getY() {
	return y;
    }
}