package yet.another.solutions.libs.random_mask_generator;

import io.github.jdiemke.triangulation.DelaunayTriangulator;
import io.github.jdiemke.triangulation.Edge2D;
import io.github.jdiemke.triangulation.NotEnoughPointsException;
import io.github.jdiemke.triangulation.Triangle2D;
import io.github.jdiemke.triangulation.Vector2D;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Vector;

public class Generator {

    protected final static double epsilon = 0.000000001d;

    public static BufferedImage createRandom(int height, int width, int minfree, int num) {
        Random random = new Random();
        int step = Math.min(height, width) / num;
        double stepD = step;
        if (step < 10) {
            throw new IllegalArgumentException("Step is too tiny. Min(height,width)/num must be greater or equal 10");
        }
        Vector<Vector2D> points = new Vector<>();
        while (points.size() < num) {
            Vector2D point = new Vector2D(
                    random.nextDouble(width),
                    random.nextDouble(height)
            );
            boolean good = true;
            if (point.x < minfree || point.y < minfree || point.x > (width - minfree) || point.y > (height - minfree)) {
                good = false;
            }
            if (good) {
                for (Vector2D existingPoint : points) {
                    if (distance(existingPoint, point) < stepD) {
                        good = false;
                    }
                }
            }
            if (good) {
                points.add(point);
            }
        }


        Vector<Vector2D> pointsOrig = new Vector<>(points);

        Tuple<List<Triangle2D>, Vector<Edge2D>> soupTuple = null;
        int desiredNum = num;
        List<Triangle2D> triangleSoup = null;
        Vector<Edge2D> outerEdges = null;

        soupTuple = trySoup(points);
        triangleSoup = soupTuple.a;
        outerEdges = soupTuple.b;
        while (outerEdges.size() < num) {
            triangleSoup.remove(getOuterTriangle(triangleSoup, points));
            outerEdges = getOuterEdges(triangleSoup);
        }

//        dumpSoup(triangleSoup, points, outerEdges, 1000, 1000);

        Vector<Vector2D> outerPoints = calculatePoints(
                outerEdges, new Vector<>()
        );

//        Vector<Vector2D> outerPoints = new Vector<>();
//        Edge2D firstEdge = outerEdges.firstElement();
//        outerPoints.add(firstEdge.a);
//        outerPoints.add(firstEdge.b);
//        outerEdges.remove(firstEdge);
//        do {
//            dumpPoints(outerPoints, 1000, 1000);
//            boolean fine = false;
//            for (Edge2D edge : new Vector<>(outerEdges)) {
//                if (pointBlank(outerPoints.lastElement(), edge.a)) {
//                    outerPoints.add(edge.b);
//                    outerEdges.remove(edge);
//                    fine = true;
//                    break;
//                } else if (pointBlank(outerPoints.lastElement(), edge.b)) {
//                    outerPoints.add(edge.a);
//                    outerEdges.remove(edge);
//                    fine = true;
//                    break;
//                }
//            }
//            if (!fine) {
//                System.out.println("!!!");
//                //when all goes wrong
//                firstEdge = outerEdges.firstElement();
//                outerPoints.add(firstEdge.a);
//                outerPoints.add(firstEdge.b);
//                outerEdges.remove(firstEdge);
//            }
//        } while (!outerEdges.isEmpty());

        outerPoints.remove(outerPoints.lastElement());

        BufferedImage canvas = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = canvas.createGraphics();

        graphics.setBackground(Color.WHITE);
        graphics.fillRect(0, 0, width, height);
        graphics.setColor(Color.BLACK);
        BasicStroke bs = new BasicStroke(1);
        graphics.setStroke(bs);

        Polygon polygon = new Polygon();
        outerPoints.forEach(vector2D -> polygon.addPoint(Long.valueOf(Math.round(vector2D.x)).intValue(), Long.valueOf(Math.round(vector2D.y)).intValue()));

        graphics.fillPolygon(polygon);

//        graphics.setColor(Color.GREEN);
//        graphics.drawPolygon(polygon);
//
//        graphics.setColor(Color.RED);
//        bs = new BasicStroke(5);
//        graphics.setStroke(bs);
//        for (Vector2D point : pointsOrig) {
//            graphics.drawLine(
//                    Double.valueOf(point.x).intValue(), Double.valueOf(point.y).intValue(),
//                    Double.valueOf(point.x).intValue(), Double.valueOf(point.y).intValue()
//            );
//        }







        return canvas;

    }

    protected static Vector<Vector2D> calculatePoints(Vector<Edge2D> edges, Vector<Vector2D> points) {
        if (points.isEmpty()) {
            Vector<Edge2D> edgesInt = new Vector<>(edges);
            Edge2D firstEdge = edges.firstElement();
            Vector<Vector2D> pointsInt = new Vector<>(points);
            pointsInt.add(firstEdge.a);
            pointsInt.add(firstEdge.b);
            edgesInt.remove(firstEdge);
            return calculatePoints(edgesInt, pointsInt);
        } else {
            if (edges.isEmpty()) {
                return points;
            }
//            dumpPoints(points, 1000, 1000);
            Vector<Edge2D> edgesInt = new Vector<>(edges);
            Vector<Vector2D> pointsInt = new Vector<>(points);
            boolean fine = false;
            for (Edge2D edge : new Vector<>(edgesInt)) {
                if (pointBlank(pointsInt.lastElement(), edge.a)) {
                    pointsInt.add(edge.b);
                    edgesInt.remove(edge);
                    fine = true;
                } else if (pointBlank(pointsInt.lastElement(), edge.b)) {
                    pointsInt.add(edge.a);
                    edgesInt.remove(edge);
                    fine = true;
                }
                Vector<Vector2D> res = null;
                if (fine) {
                    res = calculatePoints(edgesInt, pointsInt);
                }
                if (res != null) {
                    return res;
                } else {
                    fine = false;
                    edgesInt = new Vector<>(edges);
                    pointsInt = new Vector<>(points);
                }
            }
            if (fine) {
                return pointsInt;
            } else {
                return null;
            }
        }
    }


    protected static Tuple<List<Triangle2D>, Vector<Edge2D>> trySoup(Vector<Vector2D> points) {
        DelaunayTriangulator delaunayTriangulator = new DelaunayTriangulator(points);
        try {
            delaunayTriangulator.triangulate();
        } catch (NotEnoughPointsException e) {
            throw new IllegalStateException(e);
        }

        List<Triangle2D> triangleSoup = delaunayTriangulator.getTriangles();

        Vector<Edge2D> outerEdges = getOuterEdges(triangleSoup);
        return new Tuple<>(triangleSoup, outerEdges);
    }

    private static Triangle2D getOuterTriangle(List<Triangle2D> triangleSoup, Vector<Vector2D> points) {
        Vector<Edge2D> outerEdges = getOuterEdges(triangleSoup);
        Map<Triangle2D, Integer> map = new HashMap<>();
        for (Edge2D edge : outerEdges) {
            for (Triangle2D triangle : triangleSoup) {
                if (triangle.isNeighbour(edge)) {
                    if (map.containsKey(triangle)) {
                        map.put(triangle, map.get(triangle) + 1);
                    } else {
                        map.put(triangle, 1);
                    }
                }
            }
        }

//        dumpSoup(triangleSoup, points, outerEdges, 1000, 1000);

//        for (int i = 1; i < 3; i++) {
            for (Map.Entry<Triangle2D, Integer> entry : map.entrySet()) {
                if (entry.getValue() == 1) {
                    int hits = 0;
                    for (Vector2D point : points) {
                        if (hasVertex(entry.getKey(), point)) {
                            hits ++;
                        }
                    }
                    if (hits < 3) {
                        return entry.getKey();
                    }
                }
            }
//        }
//        for (int i = 1; i < 3; i++) {
            for (Map.Entry<Triangle2D, Integer> entry : map.entrySet()) {
                if (entry.getValue() == 1) {
                    int hits = 0;
                    for (Vector2D point : points) {
                        if (hasVertex(entry.getKey(), point)) {
                            hits ++;
                        }
                    }
                    if (hits == 3) {
                        return entry.getKey();
                    }
                }
            }
//        }

//        dumpSoup(triangleSoup, points, outerEdges, 1000, 1000);

        throw new IllegalStateException("Cannot find triangle to delete");
    }

    private static void dumpPoints(Vector<Vector2D> points, int width, int height) {
        int step = points.size();
        Vector<Vector2D> pointsInt = new Vector<>(points);
        BufferedImage canvas = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = canvas.createGraphics();

        graphics.setBackground(Color.WHITE);
        graphics.fillRect(0, 0, width, height);
        graphics.setColor(Color.BLACK);
        BasicStroke bs = new BasicStroke(1);
        graphics.setStroke(bs);

        Path2D path = new Path2D.Double();
        Vector2D init = pointsInt.firstElement();
        path.moveTo(init.x, init.y);
        pointsInt.remove(pointsInt);
        for (Vector2D point : pointsInt) {
            path.lineTo(point.x, point.y);
        }

        graphics.draw(path);

        try {
            ImageIO.write(canvas, "png", new File("./edges"+step+".png"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    private static void dumpSoup(List<Triangle2D> soup, Vector<Vector2D> points, Vector<Edge2D> outerEdges, int width, int height) {
        BufferedImage canvas = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = canvas.createGraphics();

        graphics.setBackground(Color.WHITE);
        graphics.fillRect(0, 0, width, height);
        graphics.setColor(Color.BLACK);
        BasicStroke bs = new BasicStroke(1);
        graphics.setStroke(bs);

        for (Triangle2D triangle: soup) {
            Polygon polygon = new Polygon();
            polygon.addPoint(
                    Double.valueOf(triangle.a.x).intValue(), Double.valueOf(triangle.a.y).intValue()
            );
            polygon.addPoint(
                    Double.valueOf(triangle.b.x).intValue(), Double.valueOf(triangle.b.y).intValue()
            );
            polygon.addPoint(
                    Double.valueOf(triangle.c.x).intValue(), Double.valueOf(triangle.c.y).intValue()
            );
            graphics.drawPolygon(polygon);
        }

        graphics.setColor(Color.RED);
        bs = new BasicStroke(5);
        graphics.setStroke(bs);
        for (Vector2D point : points) {
            graphics.drawLine(
                    Double.valueOf(point.x).intValue(), Double.valueOf(point.y).intValue(),
                    Double.valueOf(point.x).intValue(), Double.valueOf(point.y).intValue()
            );
        }

        graphics.setColor(Color.GREEN);
        bs = new BasicStroke(1);
        graphics.setStroke(bs);

        for (Edge2D edge : outerEdges) {
            graphics.drawLine(
                    Double.valueOf(edge.a.x).intValue(), Double.valueOf(edge.a.y).intValue(),
                    Double.valueOf(edge.b.x).intValue(), Double.valueOf(edge.b.y).intValue()
            );
        }

        try {
            ImageIO.write(canvas, "png", new File("./soup.png"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    private static boolean hasVertex(Triangle2D triangle, Vector2D point) {
        return pointBlank(triangle.a, point) || pointBlank(triangle.b, point) || pointBlank(triangle.c, point);
    }

    private static Vector<Edge2D> getOuterEdges(List<Triangle2D> triangleSoup) {
        Vector<Edge2D> outerEdges = new Vector<>();

        for (Triangle2D candidate : triangleSoup) {
            List<Edge2D> edges = new ArrayList<>();
            edges.add(new Edge2D(candidate.a, candidate.b));
            edges.add(new Edge2D(candidate.b, candidate.c));
            edges.add(new Edge2D(candidate.c, candidate.a));
            List<Edge2D> edgeToDelete = new ArrayList<>();
            for (Triangle2D triangle : triangleSoup) {
                if (!triangle.equals(candidate)) {
                    for (Edge2D edge : edges) {
                        if (triangle.isNeighbour(edge)) {
                            edgeToDelete.add(edge);
                        }
                    }
                }
            }
            edges.removeAll(edgeToDelete);
            //not needed, there can be triangles completely inside
//            if (edges.size() > 2) {
//                throw new IllegalStateException("Strange...");
//            }
            if (edges.size() > 0) {
                outerEdges.addAll(edges);
            }
        }
        return outerEdges;
    }

    protected static double distance(Vector2D one, Vector2D two) {
        return Math.sqrt(
                (one.x - two.x) * (one.x - two.x) + (one.y - two.y) * (one.y - two.y)
        );
    }

    protected static boolean pointBlank(Vector2D one, Vector2D two) {
        //should use epsilon?
        return distance(one, two) < epsilon;
//        return (one.x == two.x) && (one.y == two.y);
    }

    protected static class Tuple<A,B> {
        A a;
        B b;

        public Tuple(A a, B b) {
            this.a = a;
            this.b = b;
        }
    }

}
