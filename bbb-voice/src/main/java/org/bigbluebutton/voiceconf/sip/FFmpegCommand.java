package org.bigbluebutton.voiceconf.sip;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;

public class FFmpegCommand {
    private HashMap args;
    private HashMap x264Params;
    private List<String[]> rtmpInputConnParams;
    private List<String[]> rtmpOutputConnParams;

    private String[] command;
    
    private String ffmpegPath;
    private String input;
    private String output;
    private Boolean inputLive;

    /* Analyze duration is a special parameter that MUST come before the input */
    private String analyzeDuration;
    private String probeSize;

    /* Parameters when the input is a loop image/file */
    private String loop;
    private String frameRate;
    private String frameSize;

    public FFmpegCommand() {
        this.args = new HashMap();
        this.x264Params = new HashMap();
        this.rtmpInputConnParams = new ArrayList<String[]>();
        this.rtmpOutputConnParams = new ArrayList<String[]>();


        this.ffmpegPath = null;
        this.inputLive = false;
        this.loop = null;
    }

    public String[] getFFmpegCommand(boolean shouldBuild) {
        if(shouldBuild)
            buildFFmpegCommand();
        
        return this.command;
    }

    public void buildFFmpegCommand() {
        List comm = new ArrayList<String>();

        if(this.ffmpegPath == null)
            this.ffmpegPath = "/usr/local/bin/ffmpeg";

        comm.add(this.ffmpegPath);

        if(frameRate != null && !frameRate.isEmpty()){
            comm.add("-framerate");
            comm.add(frameRate);
        }

        if (this.inputLive){
            comm.add("-re");
        }

        /* Analyze duration and probesize MUST come before the input */
        if(analyzeDuration != null && !analyzeDuration.isEmpty()) {
            comm.add("-analyzeduration");
            comm.add(analyzeDuration);
        }

        if(loop != null && !loop.isEmpty()){
            comm.add("-loop");
            comm.add(loop);
        }

        if(probeSize != null && !probeSize.isEmpty()) {
            comm.add("-probesize");
            comm.add(probeSize);
        }

        buildRtmpInput();

        comm.add("-i");
        comm.add(input);

        Iterator argsIter = this.args.entrySet().iterator();
        while (argsIter.hasNext()) {
            Map.Entry pairs = (Map.Entry)argsIter.next();
            comm.add(pairs.getKey());
            comm.add(pairs.getValue());
        }

        if(!x264Params.isEmpty()) {
            comm.add("-x264-params");
            String params = "";
            Iterator x264Iter = this.x264Params.entrySet().iterator();
            while (x264Iter.hasNext()) {
                Map.Entry pairs = (Map.Entry)x264Iter.next();
                String argValue = pairs.getKey() + "=" + pairs.getValue();
                params += argValue;
                // x264-params are separated by ':'
                params += ":";
            }
            // Remove trailing ':'
            params.replaceAll(":+$", "");
            comm.add(params);
        }

        buildRtmpOutput();

        comm.add(this.output);

        this.command = new String[comm.size()];
        comm.toArray(this.command);
    }

    /**
     * Add rtmp parameters (if there are any) to the current input,
     * if the input is rtmp.
     */
    private void buildRtmpInput() {
        if(!rtmpInputConnParams.isEmpty() && isRtmpInput()) {
            StringBuilder sb = new StringBuilder();
            for (String s[] : rtmpInputConnParams){
                sb.append("conn="+s[0]+":"+s[1]+" ");
            }
            input+=" "+sb.toString().trim();
        }
    }

    /**
     * Add rtmp parameters (if there are any) to the current output,
     * if the output is rtmp.
     */
    private void buildRtmpOutput() {
        if(!rtmpOutputConnParams.isEmpty() && isRtmpOutput()) {
            StringBuilder sb = new StringBuilder();
            for (String s[] : rtmpOutputConnParams){
                sb.append("conn="+s[0]+":"+s[1]+" ");
            }
            output+=" "+sb.toString().trim();
        }
    }


    public void setFFmpegPath(String arg) {
        this.ffmpegPath = arg;
    }

    public void setInput(String arg) {
        this.input = arg;
    }

    public void setInputLive(Boolean live){
        this.inputLive = live;
    }

    public void setOutput(String arg) {
        this.output = arg;
    }

    public void setCodec(String arg) {
        this.args.put("-vcodec", arg);
    }

    public void setLoop(String arg) {
        this.loop = arg;
    }

    public void setLevel(String arg) {
        this.args.put("-level", arg);
    }

    public void setPreset(String arg) {
        this.args.put("-preset", arg);
    }

    public void setProfile(String arg) {
        this.args.put("-profile:v", arg);
    }

    public void setFormat(String arg) {
        this.args.put("-f", arg);
    }

    public void setPayloadType(String arg) {
        this.args.put("-payload_type", arg);
    }

    public void setLoglevel(String arg) {
        this.args.put("-loglevel", arg);
    }

    public void setPixelFormat(String arg){
        this.args.put("-pix_fmt", arg);
    }
    public void setSliceMaxSize(String arg) {
        this.x264Params.put("slice-max-size", arg);
    }

    public void setMaxKeyFrameInterval(String arg) {
        this.x264Params.put("keyint", arg);
    }

    public void addCustomParameter(String name, String value) {
        this.args.put(name, value);
    }

   /**
    * Set how much time FFmpeg should  analyze stream
    * data to get stream information. Note that this
    * affects directly the delay to start the stream.
    *
    * @param duration Analysis duration
    */
   public void setAnalyzeDuration(String duration) {
       this.analyzeDuration = duration;
   }

   /**
    * Probe size, in bytes.
    * Minimum value: 32
    * Default: 5000000
    **/
   public void setProbeSize(String size) {
       this.probeSize = size;
   }

   /**
    * Set frame rate of the input data
    * @param value
    */
   public void setFrameRate(String value){
       this.frameRate = value;
   }

   public void setFrameSize(String value){
       this.frameSize = value;
   }

   /**
    * Add parameters for rtmp connections.
    * The order of parameters is the order they are added
    * @param value
    */
   public void addRtmpInputConnectionParameter(String value){
       //S: String
       this.rtmpInputConnParams.add(new String[]{"S", value});
   }

   /**
    * Add parameters for rtmp connections.
    * The order of parameters is the order they are added
    * @param value
    */
   public void addRtmpInputConnectionParameter(boolean value){
       //B: Boolean
       this.rtmpInputConnParams.add(new String[]{"B", value?"1":"0"});
   }

   /**
    * Add parameters for rtmp connections.
    * The order of parameters is the order they are added
    * @param value
    */
   public void addRtmpInputConnectionParameter(int value){
       //N : Number
       this.rtmpInputConnParams.add(new String[]{"N", Integer.toString(value)});
   }

   /**
    * Add parameters for rtmp connections.
    * The order of parameters is the order they are added
    * @param value
    */
   public void addRtmpOutputConnectionParameter(String value){
       //S: String
       this.rtmpOutputConnParams.add(new String[]{"S", value});
   }

   /**
    * Add parameters for rtmp connections.
    * The order of parameters is the order they are added
    * @param value
    */
   public void addRtmpOutputConnectionParameter(boolean value){
       //B: Boolean
       this.rtmpOutputConnParams.add(new String[]{"B", value?"1":"0"});
   }

   /**
    * Add parameters for rtmp connections.
    * The order of parameters is the order they are added
    * @param value
    */
   public void addRtmpOutputConnectionParameter(int value){
       //N : Number
       this.rtmpOutputConnParams.add(new String[]{"N", Integer.toString(value)});
   }

   /**
    * Check if the current set intput is rtmp
    * @return
    */
   private boolean isRtmpInput(){
       return input.contains("rtmp");
   }

   /**
    * Check if the current set output is rtmp
    * @return
    */
   private boolean isRtmpOutput(){
       return output.contains("rtmp");
   }
}
