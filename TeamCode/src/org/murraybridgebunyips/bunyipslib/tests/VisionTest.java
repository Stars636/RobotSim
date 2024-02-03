package org.murraybridgebunyips.bunyipslib.tests;

import static org.murraybridgebunyips.bunyipslib.bunyipsftc.personalitycore.CompanionCubeColours.BLUE_ELEMENT_B;
import static org.murraybridgebunyips.bunyipslib.bunyipsftc.personalitycore.CompanionCubeColours.BLUE_ELEMENT_G;
import static org.murraybridgebunyips.bunyipslib.bunyipsftc.personalitycore.CompanionCubeColours.BLUE_ELEMENT_R;
import static org.murraybridgebunyips.bunyipslib.bunyipsftc.personalitycore.CompanionCubeColours.RED_ELEMENT_B;
import static org.murraybridgebunyips.bunyipslib.bunyipsftc.personalitycore.CompanionCubeColours.RED_ELEMENT_G;
import static org.murraybridgebunyips.bunyipslib.bunyipsftc.personalitycore.CompanionCubeColours.RED_ELEMENT_R;

import com.acmerobotics.dashboard.FtcDashboard;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.robotcore.external.hardware.camera.WebcamName;
import org.murraybridgebunyips.bunyipslib.BunyipsOpMode;
import org.murraybridgebunyips.bunyipslib.EmergencyStop;
import org.murraybridgebunyips.bunyipslib.UserSelection;
import org.murraybridgebunyips.bunyipslib.cameras.C920;
import org.murraybridgebunyips.bunyipslib.vision.Processor;
import org.murraybridgebunyips.bunyipslib.vision.SwitchableVisionSender;
import org.murraybridgebunyips.bunyipslib.vision.Vision;
import org.murraybridgebunyips.bunyipslib.vision.processors.AprilTag;
import org.murraybridgebunyips.bunyipslib.vision.processors.TFOD;
import org.murraybridgebunyips.bunyipslib.vision.processors.TeamProp;
import org.murraybridgebunyips.bunyipslib.vision.processors.WhitePixel;

import kotlin.Unit;

/**
 * Test Vision processor detections and data throughput
 * Compatible with all robots with a hardware device webcam "webcam"
 *
 * @author Lucas Bubner, 2023
 */
@TeleOp(name = "Vision Test")
//@Disabled
public class VisionTest extends BunyipsOpMode {
    private Vision vision;
    private Telemetry.Item i;
    private SwitchableVisionSender visionSender;
    private final UserSelection<Procs> procChooser = new UserSelection<>(this, this::callback, Procs.values());

    @SuppressWarnings("rawtypes")
    private Unit callback(Procs selection) {
        if (selection == null) {
            return Unit.INSTANCE;
        }
        Processor chosenProcessor = null;
        switch (selection) {
            case TFOD:
                chosenProcessor = new TFOD();
                break;
            case APRILTAG:
                chosenProcessor = new AprilTag(new C920());
                break;
            case TEAMPROP_RED:
                chosenProcessor = new TeamProp(RED_ELEMENT_R, RED_ELEMENT_G, RED_ELEMENT_B);
                break;
            case TEAMPROP_BLUE:
                chosenProcessor = new TeamProp(BLUE_ELEMENT_R, BLUE_ELEMENT_G, BLUE_ELEMENT_B);
                break;
            case WHITE_PIXEL:
                chosenProcessor = new WhitePixel();
                break;
        }

        vision.init(chosenProcessor);
        vision.start(chosenProcessor);
        visionSender = new SwitchableVisionSender(this, chosenProcessor);
        visionSender.setStreamingProcessor(chosenProcessor.getName());

        i = addRetainedTelemetry("Camera Stream available.");
        return Unit.INSTANCE;
    }

    @Override
    protected boolean onInitLoop() {
        return !procChooser.isAlive();
    }

    @Override
    protected void onInit() {
        try {
            WebcamName webcam = (WebcamName) hardwareMap.get("webcam");
            vision = new Vision(this, webcam);
        } catch (IllegalArgumentException e) {
            throw new EmergencyStop("VisionTest is missing a webcam called 'webcam'!");
        }
        procChooser.start();
    }

    @Override
    protected void onStart() {
        if (vision == null) {
            exit();
        }
        removeTelemetryItems(i);
    }

    @Override
    protected void activeLoop() {
        vision.update();
        visionSender.update();
        addTelemetry(String.valueOf(vision.getAllData()));
    }

    @Override
    protected void onStop() {
        FtcDashboard.getInstance().stopCameraStream();
        if (vision != null)
            vision.terminate();
    }

    private enum Procs {
        TFOD,
        APRILTAG,
        TEAMPROP_RED,
        TEAMPROP_BLUE,
        WHITE_PIXEL
    }
}