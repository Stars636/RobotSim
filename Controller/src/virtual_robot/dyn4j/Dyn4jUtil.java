package virtual_robot.dyn4j;

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import org.dyn4j.dynamics.Body;
import org.dyn4j.dynamics.BodyFixture;
import org.dyn4j.geometry.Convex;
import org.dyn4j.geometry.MassType;
import org.dyn4j.geometry.Vector2;
import virtual_robot.controller.VirtualField;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 *  Static utility functions for using dyn4j in conjunction with JavaFX
 *
 */
public class Dyn4jUtil {

    private static final double PIXELS_PER_METER = (75.0 / 18.0) / 0.0254;

    /**
     * Return the position (meters, Y-up) of the center of the specified Shape object, relative to the
     * (xOffset, yOffset) position (inches, Y-down).
     * @param shape             The Rectangle, Circle, or Ellipse in question
     * @param xOffsetInches     Reference X-value (inches)
     * @param yOffsetInches     Reference Y-value (inches - in a Y-down system)
     * @return                  Offset (meters, Y-up, of center of shape relative to reference point)
     *
     * The most common use would be to determine the position of a shape in the robot fxml file relative to
     * the center of the robot, in meters, world coordinates (i.e., Y-up). In that case, use xOffset = 9
     * and yOffset = 9 (for 18 inch robot).
     */
    public static Vector2 getCenterMeters(Shape shape, double xOffsetInches, double yOffsetInches){
        double xOffsetMeters = xOffsetInches / VirtualField.INCHES_PER_METER;
        double yOffsetMeters = yOffsetInches / VirtualField.INCHES_PER_METER;
        double xCenterMeters = 0;
        double yCenterMeters = 0;
        if (shape instanceof Rectangle){
            Rectangle r = (Rectangle)shape;
            double widthMeters = r.getWidth() / PIXELS_PER_METER;
            double heightMeters = r.getHeight() / PIXELS_PER_METER;
            xCenterMeters = (r.getX() + r.getTranslateX()) / PIXELS_PER_METER + widthMeters / 2.0 - xOffsetMeters;
            yCenterMeters = yOffsetMeters - (r.getY() + r.getTranslateY()) / PIXELS_PER_METER - heightMeters / 2.0;
        } else if (shape instanceof Circle) {
            Circle c = (Circle)shape;
            xCenterMeters = (c.getCenterX() + c.getTranslateX()) / PIXELS_PER_METER - xOffsetMeters;
            yCenterMeters = yOffsetMeters - (c.getCenterY() + c.getTranslateY()) / PIXELS_PER_METER;
        } else if (shape instanceof Ellipse) {
            Ellipse e = (Ellipse)shape;
            xCenterMeters = (e.getCenterX() + e.getTranslateX()) / PIXELS_PER_METER - xOffsetMeters;
            yCenterMeters = yOffsetMeters - (e.getCenterY() + e.getTranslateY()) / PIXELS_PER_METER;
        } else {
            throw new IllegalArgumentException("Argument must be Rectangle, Circle, or Ellipse.");
        }
        return new Vector2(xCenterMeters, yCenterMeters);
    }

    /**
     * Get position (in meters, Y-up) of center of the shape relative to the center of an 18x18 inch robot
     * chassis.
     * @param shape
     * @return
     */
    public Vector2 getCenterMeters(Shape shape){
        return getCenterMeters(shape, 9, 9);
    }


    /**
     * Create a BodyFixture corresponding to the JavaFX Shape.
     * If applyTransforms is true, The Convex object of the BodyFixture will be transformed so that it is rotated
     * and translated (relative to the point xOffsetInches, yOffsetInches) to correspond to the rotation and
     * position of the JavaFX Shape.
     * @param shape
     * @param xOffsetInches
     * @param yOffsetInches
     * @param applyTransforms
     * @param fixtureData
     * @return
     */
    public static BodyFixture createFixture(Shape shape, double xOffsetInches, double yOffsetInches,
                                            boolean applyTransforms, FixtureData fixtureData){
        Convex convex = null;

        if (fixtureData == null) {
            fixtureData = new FixtureData();
        }

        if (shape instanceof Rectangle) {
            Rectangle r = (Rectangle) shape;
            double widthMeters = r.getWidth() / PIXELS_PER_METER;
            double heightMeters = r.getHeight() / PIXELS_PER_METER;
            convex = new org.dyn4j.geometry.Rectangle(widthMeters, heightMeters);
        } else if (shape instanceof Circle) {
            Circle c = (Circle) shape;
            double radMeters = c.getRadius() / PIXELS_PER_METER;
            convex = new org.dyn4j.geometry.Circle(radMeters);
        } else if (shape instanceof Ellipse) {
            Ellipse e = (Ellipse) shape;
            double widthMeters = 2.0 * e.getRadiusX() / PIXELS_PER_METER;
            double heightMeters = 2.0 * e.getRadiusY() / PIXELS_PER_METER;
            convex = new org.dyn4j.geometry.Ellipse(widthMeters, heightMeters);
        } else {
            throw new IllegalArgumentException("Shape must be Rectangle, Circle, or Ellipse.");
        }

        if (applyTransforms){
            double radians = -Math.toRadians(shape.getRotate());
            convex.rotate(radians);
            Vector2 translate = getCenterMeters(shape, xOffsetInches, yOffsetInches);
            convex.translate(translate);
        }

        BodyFixture bodyFixture = new BodyFixture(convex);
        if (fixtureData.filter != null){
            bodyFixture.setFilter(fixtureData.filter);
        }
        bodyFixture.setRestitution(fixtureData.restitution);
        bodyFixture.setFriction(fixtureData.friction);
        bodyFixture.setDensity(fixtureData.density);
        bodyFixture.setSensor(fixtureData.isSensor);

        bodyFixture.setUserData(shape.getId());

        return bodyFixture;
    }

    public static BodyFixture createFixture(Shape shape, boolean applyTransforms, FixtureData fixtureData){
        return createFixture(shape, 9, 9, applyTransforms, fixtureData);
    }

    /**
     * Obtain List of BodyFixtures corresponding to the JavaFX Shapes within the JavaFX Group.
     * These BodyFixtures will have their Convex objects transformed, based on the supplied
     * (xOffset, yOffset) point, and the positions and rotations of the Shapes within the Group.
     * @param group
     * @param xOffsetInches
     * @param yOffsetInches
     * @param fixtureData
     * @return
     */
    public static List<BodyFixture> createFixtures(Group group, double xOffsetInches, double yOffsetInches,
                                                   FixtureData fixtureData){
        List<BodyFixture> list = new ArrayList<>();
        FixtureData data = fixtureData == null? new FixtureData() : fixtureData;

        for (Node n: group.getChildren()){
            if (n instanceof Shape){
                BodyFixture fixture = createFixture((Shape)n, xOffsetInches, yOffsetInches, true, data);
                list.add(fixture);
            }else {
                throw new IllegalArgumentException("Group children must be Shapes.");
            }
        }
        return list;
    }

    public static List<BodyFixture> createFixtures(Group group, FixtureData fixtureData){
        return createFixtures(group, 9, 9, fixtureData);
    }

    public static List<BodyFixture> createFixtures(Group group, double xOffsetInches, double yOffsetInches,
                                                   Map<Shape,FixtureData> map){
        List<BodyFixture> list = new ArrayList<>();
        FixtureData defaultData = new FixtureData();

        for (Node n: group.getChildren()){
            if (n instanceof Shape){
                FixtureData data = defaultData;
                if (map != null &&  map.get((Shape)n) != null){
                    data = map.get((Shape)n);
                }
                BodyFixture fixture = createFixture((Shape)n, xOffsetInches, yOffsetInches, true, data);
                list.add(fixture);
            }else {
                throw new IllegalArgumentException("Group children must be Shapes.");
            }
        }
        return list;
    }

    public static List<BodyFixture> createFixtures(Group group, Map<Shape, FixtureData> map) {
        return createFixtures(group, 9, 9, map);
    }

    /**
     * Create a Body containing a single BodyFixture, based on the provided JavaFX Shape. The Body will be
     * positioned (relative to the provided xOffsetInches, yOffsetInches) and rotated based on the
     * position/rotation of the JavaFX Shape.
     * @param shape
     * @param xOffsetInches
     * @param yOffsetInches
     * @param fixtureData
     * @return
     */
    public static Body createBody(Shape shape, double xOffsetInches, double yOffsetInches, FixtureData fixtureData){
        Body body = new Body();
        BodyFixture bodyFixture = createFixture(shape, xOffsetInches, yOffsetInches, false, fixtureData);
        Vector2 translate = getCenterMeters(shape, xOffsetInches, yOffsetInches);
        double rotate = -Math.toRadians(shape.getRotate());
        body.addFixture(bodyFixture);
        body.setMass(MassType.NORMAL);
        body.rotate(rotate);
        body.translate(translate);
        return body;
    }

    public static Body createBody(Shape shape, FixtureData fixtureData){
        return createBody(shape, 9, 9, fixtureData);
    }

    /**
     * Given a JavaFX Group of JavaFX Shapes, create a dyn4j Body that contains multiple BodyFixtures. The shapes
     * of the BodyFixture objects will be positioned/rotated based on the positions/rotations of the JavaFX Shapes.
     * The Body will be positioned based on the position of the JavaFX Group. Rotation of the Group is not allowed.
     * @param group
     * @param xOffsetInches
     * @param yOffsetInches
     * @param map
     * @return
     */
    public static Body createBody(Group group, double xOffsetInches, double yOffsetInches, Map<Shape,FixtureData> map){
        Body body = new Body();
        List<BodyFixture> fixtureList = createFixtures(group, xOffsetInches, yOffsetInches, map);
        for (BodyFixture fixture: fixtureList){
            body.addFixture(fixture);
        }
        Vector2 translate = new Vector2(group.getTranslateX() / PIXELS_PER_METER,
                -group.getTranslateY() / PIXELS_PER_METER);
        body.setMass(MassType.NORMAL);
        body.translate(translate);
        return body;
    }

    public static Body createBody(Group group, Map<Shape, FixtureData> map){
        return createBody(group, 9, 9, map);
    }

    public static Body createBody(Group group, double xOffsetInches, double yOffsetInches, FixtureData fixtureData){
        Body body = new Body();
        List<BodyFixture> fixtureList = createFixtures(group, xOffsetInches, yOffsetInches, fixtureData);
        for (BodyFixture fixture: fixtureList){
            body.addFixture(fixture);
        }
        Vector2 translate = new Vector2(group.getTranslateX() / PIXELS_PER_METER,
                -group.getTranslateY() / PIXELS_PER_METER);
        body.setMass(MassType.NORMAL);
        body.translate(translate);
        return body;
    }

    public static Body createBody(Group group, FixtureData fixtureData){
        return createBody(group, 9, 9, fixtureData);
    }

}
