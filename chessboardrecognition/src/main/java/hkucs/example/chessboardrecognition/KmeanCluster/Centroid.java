package hkucs.example.chessboardrecognition.KmeanCluster;

import org.opencv.core.Point;

public class Centroid {
    public Line line;
    public Point point;
    public Centroid(double rtho, double theta){
        line = new Line(rtho, theta);
    }
    public Centroid(Point p){
        point = p;
    }
}
