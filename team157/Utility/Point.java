package Utility;

/**
 * Primitive geometric class, HashMap compatible
 */
public class Point {
	private int x;
	private int y;

	public Point(int x, int y) {
		this.x = x;
		this.y = y;
	}

	public int getX() {
		return this.x;
	}

	public int getY() {
		return this.y;
	}

	public void setX(int x) {
		this.x = x;
	}

	public void setY(int y) {
		this.y = y;
	}

	public boolean equals(Object other) {
		if (other instanceof Point) {
			Point otherPoint = (Point) other;
			if ((this.x == otherPoint.getX()) && (this.y == otherPoint.getY())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * hash function adapted from http://www.javamex.com/tutorials/collections/hash_function_guidelines.shtml
	 * @return
	 */
	public int hashCode() {
    	return x^y;
    }
}
