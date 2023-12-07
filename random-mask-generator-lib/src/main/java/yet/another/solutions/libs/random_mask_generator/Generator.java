package yet.another.solutions.libs.random_mask_generator;

import io.github.jdiemke.triangulation.DelaunayTriangulator;
import io.github.jdiemke.triangulation.Edge2D;
import io.github.jdiemke.triangulation.NotEnoughPointsException;
import io.github.jdiemke.triangulation.Triangle2D;
import io.github.jdiemke.triangulation.Vector2D;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Vector;

public class Generator {

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
            triangleSoup.remove(getOuterTriangle(triangleSoup));
            outerEdges = getOuterEdges(triangleSoup);
        }

        Vector<Vector2D> outerPoints = new Vector<>();
        Edge2D firstEdge = outerEdges.firstElement();
        outerPoints.add(firstEdge.a);
        outerPoints.add(firstEdge.b);
        outerEdges.remove(firstEdge);
        do {
            boolean fine = false;
            for (Edge2D edge : new Vector<>(outerEdges)) {
                if (pointBlank(outerPoints.lastElement(), edge.a)) {
                    outerPoints.add(edge.b);
                    outerEdges.remove(edge);
                    fine = true;
                    break;
                } else if (pointBlank(outerPoints.lastElement(), edge.b)) {
                    outerPoints.add(edge.a);
                    outerEdges.remove(edge);
                    fine = true;
                    break;
                }
            }
            if (!fine) {
                //when all goes wrong
                firstEdge = outerEdges.firstElement();
                outerPoints.add(firstEdge.a);
                outerPoints.add(firstEdge.b);
                outerEdges.remove(firstEdge);
            }
        } while (!outerEdges.isEmpty());

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

    private static Triangle2D getOuterTriangle(List<Triangle2D> triangleSoup) {
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
        for (Map.Entry<Triangle2D, Integer> entry : map.entrySet()) {
            if (entry.getValue() == 1) {
                return entry.getKey();
            }
        }
        throw new IllegalStateException("Cannot find triangle to delete");
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
        return (one.x == two.x) && (one.y == two.y);
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
