package system.robot.subsystems.drivetrain;

import com.acmerobotics.roadrunner.drive.DriveSignal;
import com.acmerobotics.roadrunner.followers.HolonomicPIDVAFollower;
import com.acmerobotics.roadrunner.followers.TrajectoryFollower;
import com.acmerobotics.roadrunner.geometry.Pose2d;
import com.acmerobotics.roadrunner.localization.Localizer;
import com.acmerobotics.roadrunner.profile.MotionProfile;
import com.acmerobotics.roadrunner.profile.MotionProfileGenerator;
import com.acmerobotics.roadrunner.profile.MotionState;
import com.acmerobotics.roadrunner.trajectory.Trajectory;
import com.acmerobotics.roadrunner.trajectory.TrajectoryBuilder;
import com.acmerobotics.roadrunner.trajectory.constraints.*;
import com.acmerobotics.roadrunner.util.NanoClock;
import com.qualcomm.hardware.lynx.LynxModule;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.PIDFCoefficients;
import com.qualcomm.robotcore.hardware.configuration.typecontainers.MotorConfigurationType;
import org.jetbrains.annotations.NotNull;
import system.config.AutonomousConfig;
import system.config.ConfigParam;
import system.config.TeleopConfig;
import system.robot.*;
import system.robot.roadrunner_util.CoordinateMode;
import system.robot.roadrunner_util.HALTrajectory;
import system.robot.roadrunner_util.HALTrajectoryBuilder;
import system.robot.roadrunner_util.RoadrunnerConfig;
import util.control.Button;
import util.math.units.HALTimeUnit;
import util.misc.Timer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class MecanumDrive extends MecanumDriveSimple {

    private final RoadrunnerConfig rrConfig;
    private final SimpleMecanumDriveRoadrunnerController rrInterface;

    private int POSE_HISTORY_LIMIT = 100;

    private enum Mode {
        IDLE,
        TURN,
        FOLLOW_TRAJECTORY
    }

    public MecanumDrive(Robot robot, RoadrunnerConfig rrConfig, String topLeft, String topRight, String botLeft, String botRight, boolean useConfig) {
        super(robot, rrConfig, topLeft, topRight, botLeft, botRight, useConfig);

        this.rrConfig = rrConfig;

        rrInterface = new SimpleMecanumDriveRoadrunnerController();
        rrInterface.setLocalizer(localizer);
    }

    public MecanumDrive(Robot robot, RoadrunnerConfig rrConfig, String topLeft, String topRight, String botLeft, String botRight) {
        this(robot, rrConfig, topLeft, topRight, botLeft, botRight, true);
    }

    public void turnAsync(double angle) {
        rrInterface.turnAsync(angle);
    }

    public void turn(double angle) {
        rrInterface.turn(angle);
    }

    public void followTrajectoryAsync(HALTrajectory trajectory) {
        rrInterface.followTrajectoryAsync(trajectory.toRoadrunner());
    }

    public void followTrajectory(HALTrajectory trajectory) {
        rrInterface.followTrajectory(trajectory.toRoadrunner());
    }

    public Pose2d getLastError() {
        return rrInterface.getLastError();
    }

    public HALTrajectoryBuilder trajectoryBuilder(Pose2d startPose) {
        return new HALTrajectoryBuilder(
                new TrajectoryBuilder(
                        coordinateMode.convertTo(CoordinateMode.ROADRUNNER).apply(startPose),
                        rrInterface.velConstraint,
                        rrInterface.accelConstraint
                ), coordinateMode
        );
    }

    public HALTrajectoryBuilder trajectoryBuilder(Pose2d startPose, boolean reversed) {

        return new HALTrajectoryBuilder(
                new TrajectoryBuilder(
                        coordinateMode.convertTo(CoordinateMode.ROADRUNNER).apply(startPose),
                        reversed,
                        rrInterface.velConstraint,
                        rrInterface.accelConstraint
                ), coordinateMode
        );
    }

    public HALTrajectoryBuilder trajectoryBuilder(Pose2d startPose, double startHeading) {
        return new HALTrajectoryBuilder(
                new TrajectoryBuilder(
                        coordinateMode.convertTo(CoordinateMode.ROADRUNNER).apply(startPose),
                        startHeading,
                        rrInterface.velConstraint,
                        rrInterface.accelConstraint
                ), coordinateMode
        );
    }

    public void waitForRRInterfaceIdle() {
        rrInterface.waitForIdle();
    }

    public boolean rRInterfaceIsBusy() {
        return rrInterface.isBusy();
    }

    public void setMotorPIDFCoefficients(DcMotor.RunMode runMode, PIDFCoefficients pidfCoefficients) {
        rrInterface.setPIDFCoefficients(runMode, pidfCoefficients);
    }

    public void setPoseHistoryLimit(int poseHistoryLimit) {
        POSE_HISTORY_LIMIT = poseHistoryLimit;
    }

    @Override
    public void setLocalizer(Localizer localizer, CoordinateMode coordinateMode) {
        super.setLocalizer(localizer, coordinateMode);
        rrInterface.setLocalizer(localizer);
    }

    @Override
    public void setLocalizer(Localizer localizer) {
        super.setLocalizer(localizer);
        rrInterface.setLocalizer(localizer);
    }

    @AutonomousConfig
    public static ConfigParam[] autonomousConfig() {
        return new ConfigParam[] {
                new ConfigParam("Drive Mode", DriveMode.STANDARD),
                new ConfigParam("Velocity Scaling Method", SpeedScaleMethod.NONE),
                new ConfigParam("Turn Speed Scaling Method", SpeedScaleMethod.NONE),
                new ConfigParam("Velocity Multiplier", ConfigParam.numberMap(0, 10, 0.05), 1.0),
                new ConfigParam("Speed Toggle Multiplier", ConfigParam.numberMap(0, 10, 0.05), 0.5),
                new ConfigParam("Velocity Cap", ConfigParam.numberMap(0,1,0.05),1.0),
                new ConfigParam("Turn Speed Multiplier", ConfigParam.numberMap(0, 10, 0.05), 1.0),
                new ConfigParam("Turn Speed Toggle Multiplier", ConfigParam.numberMap(0, 10, 0.05), 0.5),
                new ConfigParam("Turn Speed Cap", ConfigParam.numberMap(0,1,0.05),1.0),
                new ConfigParam("Turn Button Power", ConfigParam.numberMap(0, 1, 0.05), 0.3)
        };
    }

    @TeleopConfig
    public static ConfigParam[] teleopConfig() {
        return new ConfigParam[] {
                new ConfigParam("Drive Mode", DriveMode.STANDARD),
                new ConfigParam(DRIVE_STICK, Button.VectorInputs.right_stick),
                new ConfigParam(TURN_STICK, Button.DoubleInputs.left_stick_x),
                new ConfigParam(TURN_LEFT_BUTTON, Button.BooleanInputs.noButton),
                new ConfigParam(TURN_RIGHT_BUTTON, Button.BooleanInputs.noButton),
                new ConfigParam(SPEED_TOGGLE, Button.BooleanInputs.noButton),
                new ConfigParam(TURN_SPEED_TOGGLE, Button.BooleanInputs.noButton),
                new ConfigParam("Velocity Scaling Method", SpeedScaleMethod.NONE),
                new ConfigParam("Turn Speed Scaling Method", SpeedScaleMethod.NONE),
                new ConfigParam("Velocity Multiplier", ConfigParam.numberMap(0, 10, 0.05), 1.0),
                new ConfigParam("Speed Toggle Multiplier", ConfigParam.numberMap(0, 10, 0.05), 0.5),
                new ConfigParam("Velocity Cap", ConfigParam.numberMap(0,1,0.05),1.0),
                new ConfigParam("Turn Speed Multiplier", ConfigParam.numberMap(0, 10, 0.05), 1.0),
                new ConfigParam("Turn Speed Toggle Multiplier", ConfigParam.numberMap(0, 10, 0.05), 0.5),
                new ConfigParam("Turn Speed Cap", ConfigParam.numberMap(0,1,0.05),1.0),
                new ConfigParam("Turn Button Power", ConfigParam.numberMap(0, 1, 0.05), 0.3)
        };
    }

    private class SimpleMecanumDriveRoadrunnerController extends com.acmerobotics.roadrunner.drive.MecanumDrive {

        private final NanoClock clock;
        private final Timer printTimer = new Timer();

        private Mode mode;
        private MotionProfile turnProfile;
        private double turnStart;

        private final TrajectoryVelocityConstraint velConstraint;
        private final TrajectoryAccelerationConstraint accelConstraint;
        private final TrajectoryFollower follower;

        private final LinkedList<Pose2d> poseHistory;

        //private VoltageSensor batteryVoltageSensor;

        public SimpleMecanumDriveRoadrunnerController() {
            super(rrConfig.kV,rrConfig.kA,rrConfig.kStatic,rrConfig.TRACK_WIDTH,rrConfig.WHEEL_BASE,LATERAL_MULTIPLIER);

            clock = NanoClock.system();

            mode = Mode.IDLE;

            velConstraint = new MinVelocityConstraint(Arrays.asList(
                    new AngularVelocityConstraint(rrConfig.MAX_ANG_VEL),
                    new MecanumVelocityConstraint(rrConfig.MAX_VEL, rrConfig.TRACK_WIDTH)
            ));
            accelConstraint = new ProfileAccelerationConstraint(rrConfig.MAX_ACCEL);
            follower = new HolonomicPIDVAFollower(translationCoefficients, translationCoefficients, headingCoefficients,
                    new Pose2d(rrConfig.FOLLOWER_X_TOLERANCE, rrConfig.FOLLOWER_Y_TOLERANCE, rrConfig.FOLLOWER_HEADING_TOLERANCE), rrConfig.FOLLOWER_TIMEOUT);

            poseHistory = new LinkedList<>();

            //TODO LynxModuleUtil.ensureMinimumFirmwareVersion(hardwareMap);

            //TODO batteryVoltageSensor = robot.hardwareMap.voltageSensor.iterator().next();

            for (LynxModule module : robot.hardwareMap.getAll(LynxModule.class)) {
                module.setBulkCachingMode(LynxModule.BulkCachingMode.AUTO);
            }

            for (DcMotorEx motor : motors.values()) {
                MotorConfigurationType motorConfigurationType = motor.getMotorType();
                motorConfigurationType.setAchieveableMaxRPMFraction(1.0);
                //motor.setMotorType(motorConfigurationType);
            }

            if (rrConfig.USE_DRIVE_ENCODERS) {
                setAllMotorModes(DcMotor.RunMode.RUN_USING_ENCODER);
            }

            if (rrConfig.USE_DRIVE_ENCODERS && rrConfig.MOTOR_VELO_PID != null) {
                //setPIDFCoefficients(DcMotor.RunMode.RUN_USING_ENCODER, rrConfig.MOTOR_VELO_PID);
            }
        }

        public void turnAsync(double angle) {
            double heading = getPoseEstimate().getHeading();

            turnProfile = MotionProfileGenerator.generateSimpleMotionProfile(
                    new MotionState(heading, 0, 0, 0),
                    new MotionState(heading + angle, 0, 0, 0),
                    rrConfig.MAX_ANG_VEL,
                    rrConfig.MAX_ANG_ACCEL
            );

            turnStart = clock.seconds();
            mode = Mode.TURN;
        }

        public void turn(double angle) {
            turnAsync(angle);
            waitForIdle();
        }

        public void followTrajectoryAsync(Trajectory trajectory) {
            follower.followTrajectory(trajectory);
            mode = Mode.FOLLOW_TRAJECTORY;
        }

        public void followTrajectory(Trajectory trajectory) {
            followTrajectoryAsync(trajectory);
            waitForIdle();
        }

        public Pose2d getLastError() {
            switch (mode) {
                case FOLLOW_TRAJECTORY:
                    return follower.getLastError();
                case TURN:
                    return new Pose2d(0, 0, headingController.getLastError());
                case IDLE:
                    return new Pose2d();
            }
            throw new AssertionError();
        }

        public void update() {
            updatePoseEstimate();

            Pose2d currentPose = localizerCoordinateMode.convertTo(CoordinateMode.ROADRUNNER).apply(getPoseEstimate());
            currentPose = new Pose2d(currentPose.getX(), currentPose.getY(), currentPose.getHeading());

            poseHistory.add(currentPose);

            if (POSE_HISTORY_LIMIT > -1 && poseHistory.size() > POSE_HISTORY_LIMIT) {
                poseHistory.removeFirst();
            }

            if(printTimer.requiredTimeElapsed()) {
                System.out.println(CoordinateMode.ROADRUNNER.convertTo(localizerCoordinateMode).apply(currentPose));
                printTimer.start(100, HALTimeUnit.MILLISECONDS);
            }
            switch (mode) {
                case IDLE:
                    // do nothing
                    break;
                case TURN: {
                    double t = clock.seconds() - turnStart;

                    MotionState targetState = turnProfile.get(t);

                    headingController.setTargetPosition(targetState.getX());

                    double correction = headingController.update(currentPose.getHeading());

                    double targetOmega = targetState.getV();
                    double targetAlpha = targetState.getA();
                    setDriveSignal(new DriveSignal(new Pose2d(
                            0, 0, targetOmega + correction
                    ), new Pose2d(
                            0, 0, targetAlpha
                    )));

                    if (t >= turnProfile.duration()) {
                        mode = Mode.IDLE;
                        setDriveSignal(new DriveSignal());
                    }

                    break;
                }
                case FOLLOW_TRAJECTORY: {
                    setDriveSignal(follower.update(currentPose));

                    if (!follower.isFollowing()) {
                        mode = Mode.IDLE;
                        setDriveSignal(new DriveSignal());
                    }

                    break;
                }
            }
        }

        public void waitForIdle() {
            while (!Thread.currentThread().isInterrupted() && isBusy()) {
                update();
            }
        }

        public boolean isBusy() {
            return mode != Mode.IDLE;
        }

        public void setPIDFCoefficients(DcMotor.RunMode runMode, PIDFCoefficients coefficients) {
            PIDFCoefficients compensatedCoefficients = new PIDFCoefficients(
                    coefficients.p, coefficients.i, coefficients.d,
                    coefficients.f //TODO NOTE TO SELF RE-ADD FOR ANDROID STUDIO * 12 / batteryVoltageSensor.getVoltage()
            );
            for (DcMotorEx motor : motors.values()) {
                motor.setPIDFCoefficients(runMode, compensatedCoefficients);
            }
        }

        @NotNull
        @Override
        public List<Double> getWheelPositions() {
            List<Double> wheelPositions = new ArrayList<>();
            for (DcMotorEx motor : motors.values()) {
                wheelPositions.add(rrConfig.encoderTicksToInches(motor.getCurrentPosition()));
            }
            return wheelPositions;
        }

        @Override
        public List<Double> getWheelVelocities() {
            List<Double> wheelVelocities = new ArrayList<>();
            for (DcMotorEx motor : motors.values()) {
                wheelVelocities.add(rrConfig.encoderTicksToInches(motor.getVelocity()));
            }
            return wheelVelocities;
        }

        @Override
        public void setMotorPowers(double topLeft, double botLeft, double botRight, double topRight) {
            setMotorPower(TOP_LEFT, topLeft);
            setMotorPower(BOT_LEFT, botLeft);
            setMotorPower(BOT_RIGHT, botRight);
            setMotorPower(TOP_RIGHT, topRight);
        }

        @Override
        public double getRawExternalHeading() {
            return 0;
        }
    }
}