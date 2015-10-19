package com.lazproj.devkit;


import java.awt.Frame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLProfile;
import javax.media.opengl.awt.GLCanvas;
import javax.media.opengl.glu.GLU;

import com.jogamp.opengl.util.FPSAnimator;
import com.jogamp.opengl.util.GLBuffers;

/**
 * A minimal program that draws with JOGL in an AWT Frame.
 *
 * @author Wade Walker
 */
public class Test {

    public static float angleCube = 0f;

	private static int programObject;

	private static int[] depthBufferId = new int[1];
	private static int[] locBufferId = new int[1];

	private static int[] frameBufferId1 = new int[1];
	private static int[] frameBufferId2 = new int[1];
	
	private static int shadowMapWidth = 250;
	private static int shadowMapHeight = 250;

	protected static ByteBuffer depth_pixels;
	protected static ByteBuffer loc_pixels;
	
	protected static Node previous = new Node(0,0);

	public static void main( String [] args ) {
		
        GLProfile glprofile = GLProfile.getDefault();
        GLCapabilities glcapabilities = new GLCapabilities( glprofile );
        final GLCanvas glcanvas = new GLCanvas( glcapabilities );

        glcanvas.addGLEventListener( new GLEventListener() {

			@Override
            public void reshape( GLAutoDrawable glautodrawable, int x, int y, int width, int height ) {
            	GL2 gl2 = glautodrawable.getGL().getGL2();
                GLU glu = new GLU();
            	
            	OneCube.setup( gl2, width, height );

        		int errorCode = gl2.glGetError();
        		 String errorStr = glu.gluErrorString( errorCode );
        		 System.out.println( errorStr );
        		 System.out.println( errorCode );
            }
            
            @Override
            public void init( GLAutoDrawable glautodrawable ) {
            	int width = glautodrawable.getWidth();
            	int height = glautodrawable.getHeight();
            	GL2 gl2 = glautodrawable.getGL().getGL2();
            	
    			// Compute aspect ratio of the new window
    			if (height == 0) height = 1;                // To prevent divide by 0
    				float aspect = width / height;
    			
    			// Set the viewport to cover the new window
    			gl2.glViewport(0, 0, width, height);
    			
    			// Set the aspect ratio of the clipping volume to match the viewport
    			gl2.glMatrixMode( GL2.GL_PROJECTION );  // To operate on the Projection matrix
    			gl2.glLoadIdentity();             // Reset
    			
    			// Enable perspective projection with fovy, aspect, zNear and zFar
                GLU glu = new GLU();
    			glu.gluPerspective(45.0f, aspect, 0.1f, 100.0f);
    			
    			createShaders(gl2);
        		   
        		gl2.glClearColor(0.0f, 0.0f, 0.0f, 1.0f); // Set background color to black and opaque
    			gl2.glClearDepth(1.0f);                   // Set background depth to farthest
        		gl2.glEnable( GL2.GL_DEPTH_TEST );
        		gl2.glDepthFunc( GL2.GL_LEQUAL );
        		gl2.glShadeModel( GL2.GL_SMOOTH );
        		gl2.glHint( GL2.GL_PERSPECTIVE_CORRECTION_HINT, GL2.GL_NICEST );
        		
        		//Create frame buffer
        		gl2.glGenFramebuffers(1, frameBufferId1, 0);
        		gl2.glBindFramebuffer(GL2.GL_FRAMEBUFFER, frameBufferId1[0]);

        		gl2.glGenTextures(1,locBufferId,0);
        		gl2.glBindTexture(GL2.GL_TEXTURE_2D, locBufferId[0]);

        		gl2.glTexImage2D(GL2.GL_TEXTURE_2D,          // target texture type
        		        0,                                  // mipmap LOD level
        		        GL2.GL_DEPTH_COMPONENT,         // internal pixel format
        		                                            //GL2.GL_DEPTH_COMPONENT
        		        shadowMapWidth,                     // width of generated image
        		        shadowMapHeight,                    // height of generated image
        		        0,                          // border of image
        		        GL2.GL_DEPTH_COMPONENT,     // external pixel format 
        		        GL2.GL_UNSIGNED_INT,        // datatype for each value
        		        null);  // buffer to store the texture in memory   

        		//Some parameters
        		gl2.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MIN_FILTER, GL2.GL_NEAREST);
        		gl2.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MAG_FILTER, GL2.GL_NEAREST);
        		gl2.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_S, GL2.GL_CLAMP_TO_EDGE);
        		gl2.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_T, GL2.GL_CLAMP_TO_EDGE); 

        		//Attach 2D texture to this FBO
        		gl2.glFramebufferTexture2D(GL2.GL_FRAMEBUFFER,
        		        GL2.GL_DEPTH_ATTACHMENT,
        		        GL2.GL_TEXTURE_2D,
        		        locBufferId[0],0); 
        		
        		gl2.glBindTexture(GL2.GL_TEXTURE_2D, 0);
        		
        		gl2.glDrawBuffer(GL2.GL_NONE);
        		gl2.glReadBuffer(GL2.GL_NONE);
        		
        		loc_pixels = GLBuffers.newDirectByteBuffer(shadowMapWidth*shadowMapHeight*4);
        		
        		// ------------- Depth buffer texture -------------
        		gl2.glGenFramebuffers(1, frameBufferId2, 0);
        		gl2.glBindFramebuffer(GL2.GL_FRAMEBUFFER, frameBufferId2[0]);
        		
        		gl2.glGenTextures(1,depthBufferId,0);
        		gl2.glBindTexture(GL2.GL_TEXTURE_2D, depthBufferId[0]);
        		

        		gl2.glTexImage2D(GL2.GL_TEXTURE_2D,          // target texture type
        		        0,                                  // mipmap LOD level
        		        GL2.GL_DEPTH_COMPONENT,         // internal pixel format
        		                                            //GL2.GL_DEPTH_COMPONENT
        		        shadowMapWidth,                     // width of generated image
        		        shadowMapHeight,                    // height of generated image
        		        0,                          // border of image
        		        GL2.GL_DEPTH_COMPONENT,     // external pixel format 
        		        GL2.GL_UNSIGNED_INT,        // datatype for each value
        		        null);  // buffer to store the texture in memory  

        		//Some parameters
        		gl2.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MIN_FILTER, GL2.GL_NEAREST);
        		gl2.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MAG_FILTER, GL2.GL_NEAREST);
        		gl2.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_S, GL2.GL_CLAMP_TO_EDGE);
        		gl2.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_T, GL2.GL_CLAMP_TO_EDGE);   

        		//Attach 2D texture to this FBO
        		gl2.glFramebufferTexture2D(GL2.GL_FRAMEBUFFER,
        		        GL2.GL_DEPTH_ATTACHMENT,
        		        GL2.GL_TEXTURE_2D,
        		        depthBufferId[0],0);

        		gl2.glBindTexture(GL2.GL_TEXTURE_2D, 0);

        		//Disable color buffer
        		//http://stackoverflow.com/questions/12546368/render-the-depth-buffer-into-a-texture-using-a-frame-buffer
        		gl2.glDrawBuffer(GL2.GL_NONE);
        		gl2.glReadBuffer(GL2.GL_NONE);
        		

        		//Set pixels ((width*2)* (height*2))
        		//It has to have twice the size of shadowmap size
        		depth_pixels = GLBuffers.newDirectByteBuffer(shadowMapWidth*shadowMapHeight*4);

        		//Set default frame buffer before doing the check
        		//http://www.opengl.org/wiki/FBO#Completeness_Rules
        		gl2.glBindFramebuffer(GL2.GL_FRAMEBUFFER, 0);

        		int status = gl2.glCheckFramebufferStatus(GL2.GL_FRAMEBUFFER);

        		// Always check that our framebuffer is ok
        		if(gl2.glCheckFramebufferStatus(GL2.GL_FRAMEBUFFER) != GL2.GL_FRAMEBUFFER_COMPLETE)
        		{
        		    System.err.println("Can not use FBO! Status error:" + status);
        		}
            }
            
            @Override
            public void dispose( GLAutoDrawable glautodrawable ) {
            }
            
            @Override
            public void display( GLAutoDrawable glautodrawable ) {
                GL2 gl = glautodrawable.getGL().getGL2(); // get the OpenGL graphics context

//                gl.glLoadIdentity();  // reset the model-view matrix

                //Render scene into Frame buffer first
                gl.glBindFramebuffer(GL2.GL_FRAMEBUFFER, frameBufferId2[0]);
                gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);
            	OneCube.render( gl, shadowMapWidth, shadowMapHeight );
            	
                //Read pixels from buffer
                gl.glBindFramebuffer(GL2.GL_READ_FRAMEBUFFER, frameBufferId2[0]);
//                //Read pixels 
                gl.glReadPixels(0, 0, shadowMapWidth, shadowMapHeight, GL2.GL_DEPTH_COMPONENT , GL2.GL_UNSIGNED_INT, depth_pixels);
                
                
//                //Render scene into Frame buffer first
//                gl.glBindFramebuffer(GL2.GL_FRAMEBUFFER, frameBufferId1[0]);
//                gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);
//            	OneCube.renderPoints( gl, shadowMapWidth, shadowMapHeight );
//            	
//                //Read pixels from buffer
//                gl.glBindFramebuffer(GL2.GL_READ_FRAMEBUFFER, frameBufferId1[0]);
//                //Read pixels 
//                gl.glReadPixels(0, 0, shadowMapWidth, shadowMapHeight, GL2.GL_DEPTH_COMPONENT , GL2.GL_UNSIGNED_INT, loc_pixels);

                
                //Switch back to default FBO
                gl.glBindFramebuffer(GL2.GL_FRAMEBUFFER, 0);
                gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT); 
            	OneCube.render( gl, glautodrawable.getWidth(), glautodrawable.getHeight() );


            	gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT); 
//                //Draw pixels, format has to have only one 
              	gl.glDrawPixels(shadowMapWidth, shadowMapHeight, GL2.GL_LUMINANCE , GL2.GL_UNSIGNED_INT, depth_pixels);
//              gl.glDrawPixels(shadowMapWidth, shadowMapHeight, GL2.GL_LUMINANCE , GL2.GL_UNSIGNED_INT, loc_pixels);
	              
            	IntBuffer loc_points = loc_pixels.asIntBuffer();
            	IntBuffer depth_points = depth_pixels.asIntBuffer();
            	
            	int[][] depth_map = new int[250][250];
            	
            	
            	HashMap<Integer, Node> vert_points = new HashMap<>();
            	ArrayList<Node> last_col = new ArrayList<>();
            	ArrayList<Node> this_col = new ArrayList<>();
            	
            	final int RANGE = 1000;
            	
            	// Negative Y Scan
            	float prev_depth = 0;
            	float prev_derivative = 0;
            	Node.connection_count = 0;
            	for( int x=250 ; x>0 ; x-=1 ){
            		this_col.clear();
            		for( int y=0 ; y<250 ; y+=1 ){
            			int depth_buff = depth_points.get();
            			depth_map[x-1][y] = depth_buff;
            			float derivative = (depth_buff-prev_depth);
            			if( Math.abs(derivative-prev_derivative) > RANGE ){
            				Node point = new Node(x, y);
            				this_col.add(point);
            			}
            			prev_depth = depth_buff;
            			prev_derivative = derivative;
            		}
            		
            		Node last_pt = null;
            		for( Node n : this_col ){
            			for( Node l : last_col ){
            				n.tryToConnect(l);
            			}
            			if( last_pt != null )
            				n.tryToConnect(last_pt);
            			last_pt = n;
            			
            			vert_points.put(n.x*1000+n.y,n);
            		}
            		last_col.clear();
            		last_col.addAll(this_col);
            		
            	}

            	HashMap<Integer, Node> hor_points = new HashMap<>();
            	ArrayList<Node> last_row = new ArrayList<>();
            	ArrayList<Node> this_row = new ArrayList<>();
            	
            	depth_points.rewind();
            	loc_points.rewind();
            	prev_depth = 0;
            	prev_derivative = 0;
            	Node.connection_count = 0;
        		for( int y=0 ; y<250 ; y+=1 ){
            		this_row.clear();
                	for( int x=0 ; x<250 ; x+=1 ){
            			int depth_buff = depth_map[x][y];
            			float derivative = (depth_buff-prev_depth);
            			if( Math.abs(derivative-prev_derivative) > RANGE ){
            				Node point = new Node(x, y);
            				this_row.add(point);
            			}
            			prev_depth = depth_buff;
            			prev_derivative = derivative;
            		}

            		Node last_pt = null;
            		for( Node n : this_row ){
            			for( Node l : last_row ){
            				n.tryToConnect(l);
            			}
            			if( last_pt != null )
            				n.tryToConnect(last_pt);
            			last_pt = n;
            			
            			hor_points.put(n.x*1000+n.y,n);
            		}
            		last_row.clear();
            		last_row.addAll(this_row);
            		
            	}

        		HashMap<Integer, Node> points = new HashMap<>();
        		Set<Integer> hor_keys = hor_points.keySet();
        		Set<Integer> vert_keys = vert_points.keySet();
        		
        		for( int h_k : hor_keys ){
        			Node temp = hor_points.get(h_k);
        			
        			if( vert_points.containsKey(h_k) ){
        				temp.addNeighbors( vert_points.get(h_k) );
        			}
        			
        			points.put(temp.x*1000+temp.y, temp);
        		}
        		
        		for( int v_k : vert_keys ){
        			points.put( vert_points.get(v_k).x*1000+vert_points.get(v_k).y, vert_points.get(v_k) );
        		}
        		
        		Set<Integer> p_keys = points.keySet();
        		Node curr_node = null;
        		float min_distance = Float.MAX_VALUE;
        		System.out.println(previous.x+" "+previous.y);
        		for( int i : p_keys ){
        			points.get(i).completeGraph();
        			float distance = previous.sqrdDistance(points.get(i));
        			if( distance < min_distance ){
        				curr_node = points.get(i);
        				min_distance = distance;
        			}
        		}
        		previous = curr_node;
                
                gl.glViewport(300, 0, 800, 800);
    			
                gl.glMatrixMode(GL2.GL_MODELVIEW);     // To operate on model-view matrix
 	    		   
                gl.glLoadIdentity();                 // Reset the model-view matrix
                gl.glTranslatef(0f, 0.0f, -5.0f);  // Move right and into the screen
                gl.glRotatef(0, 1.0f, 1.0f, 1.0f);  // Rotate about (1,1,1)-axis [NEW]
 	    	   
        		gl.glBegin(GL2.GL_POINTS);
        			Set<Integer> keys = points.keySet();
        			for( int i : keys ){
        				gl.glVertex2d((points.get(i).x/125.0-0.75), (points.get(i).y/125.0-0.75));
        			}
        		gl.glEnd();
        		
        		Node.result = "";
        		Node.visited.clear();
        		Node.recursion_depth = 0;
        		curr_node.getPath();
            	System.out.println("len:"+Node.result.length());
            	ThatClass.data = Node.result;
            	
            	angleCube -=10* 0.15f;
            }
        });
        
        final FPSAnimator animator = new FPSAnimator(glcanvas, 30, true);

        final Frame frame = new Frame( "One Triangle AWT" );
        frame.add( glcanvas );
        frame.addWindowListener( new WindowAdapter() {
            public void windowClosing( WindowEvent windowevent ) {
            	animator.stop();
                frame.remove( glcanvas );
                frame.dispose();
                System.exit( 0 );
            }
        });
        
        animator.start();

        frame.setSize(250, 250);
        frame.setVisible( true );
        
        Timer time = new Timer();
        time.scheduleAtFixedRate( new ThatClass(), 0, 100);
    }
	
	public static class Node{
		
		public static int connection_count = 0;
		public static HashMap< Integer, Character > visited = new HashMap<>();
		public static int max_connections = 0;
		public static int recursion_depth = 0;
		public static String result = "";
		
		private int x, y;
		private ArrayList<Node> neighbors = new ArrayList<>();
		
		public Node( int x, int y ){
			this.x = x;
			this.y = y;
		}
		
		public float sqrdDistance(Node node) {
			return (float) x*node.x + y*node.y;
		}
		
		public void addNeighbors(Node node) {
			for( Node n : node.neighbors ){
				if( !neighbors.contains(n) ){
					tryToConnect(n);
					n.forceAddNeighbors(this);
				}
			}
		}
		
		public void forceAddNeighbors(Node node){
			if( !neighbors.contains(node) ){
				neighbors.add(node);
			}
		}
		
		public void completeGraph(){
			for( Node n : neighbors ){
				n.forceAddNeighbors(this);
			}
		}

		public void tryToConnect( Node n ){
			int dx = this.x - n.x;
			int dy = this.y - n.y;
			if( Math.abs(dx) < 5 && Math.abs(dy) < 5 ){
				connection_count++;
				neighbors.add(n);
			}
			if( max_connections < neighbors.size() )
				max_connections = neighbors.size();
		}
		
		public void getPath(){
			recursion_depth++;
			if(recursion_depth < 100000){
				result += x + " " + y + " ";
					
				for( Node n : neighbors ){
					int hash = n.x*1000+n.y;
					if( !visited.containsKey(hash) ){
						visited.put(hash, ' ');
						n.getPath();
					}
				}
				result += x + " " + y + " ";
			}
		}
		
	}
	
	public static final String HOSTNAME = "192.168.42.1";
	public static final int PORT = 30000;
	public static final int TIMEOUT = 10000;
	
//	private static Socket piSocket;
	
	private static class ThatClass extends TimerTask{
		
		private PrintWriter out;
		public static String data = "";
		
		public ThatClass(){
			try {
					Socket piSocket = new Socket();
					piSocket.connect(new InetSocketAddress(HOSTNAME, PORT), 3);
					out = new PrintWriter(piSocket.getOutputStream(), true);
					BufferedReader in = new BufferedReader( new InputStreamReader(piSocket.getInputStream()));
					
			} catch (UnknownHostException e){
				System.err.println("Don't know about host " + HOSTNAME);
	            
			} catch (IOException e) {
				System.err.println("Couldn't get I/O for the connection to " + HOSTNAME);
			}
		}
		
		@Override
		public void run() {
//			System.out.println("data:"+data);
			out.write(data+"\n");
			out.flush();
		}
			
	}
	
	
	private static void createShaders(GL2 gl)
	{
		programObject = gl.glCreateProgram();
		
		int vertexShaderObject;
		int fragmentShaderObject;
		
		vertexShaderObject = gl.glCreateShader(GL2.GL_VERTEX_SHADER);
		fragmentShaderObject = gl.glCreateShader(GL2.GL_FRAGMENT_SHADER);
		
		String[] vertData,fragData;
		
		vertData = loadShader("/shaders/do_nothing.vert");
		fragData = loadShader("/shaders/do_nothing.frag");
		
		gl.glShaderSource(vertexShaderObject, 1, vertData, null, 0);
		gl.glShaderSource(fragmentShaderObject, 1, fragData, null, 0);
		
		gl.glCompileShader(vertexShaderObject);
		gl.glCompileShader(fragmentShaderObject);
		
		gl.glAttachShader(programObject, vertexShaderObject);
		gl.glAttachShader(programObject, fragmentShaderObject);
		gl.glLinkProgram(programObject);
		gl.glValidateProgram(programObject);
		
		int i;
		if((i=gl.glGetError())!=0)
		{
			System.err.println("Error in shader creation:" + i);
		}
		
		gl.glUseProgram(programObject);
	}
	
	// loads the shaders
    // in this example we assume that the shader is a file located in the applications JAR file.
    //
    public static String[] loadShader( String name )
    {
        StringBuilder sb = new StringBuilder();
        try
        {
            InputStream is = Test.class.getResourceAsStream(name);
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String line;
            while ((line = br.readLine()) != null)
            {
                sb.append(line);
                sb.append('\n');
            }
            is.close();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        //System.out.println("Shader is " + sb.toString());
        return new String[]
        { sb.toString() };
    }
    
    public static class OneCube {

		protected static void setup( GL2 gl2, int width, int height ){
			// Compute aspect ratio of the new window
			if (height == 0) height = 1;                // To prevent divide by 0
				float aspect = width / height;
			
			// Set the viewport to cover the new window
			gl2.glViewport(0, 0, width, height);
			
			// Set the aspect ratio of the clipping volume to match the viewport
			gl2.glMatrixMode( GL2.GL_PROJECTION );  // To operate on the Projection matrix
			gl2.glLoadIdentity();             // Reset
			
			// Enable perspective projection with fovy, aspect, zNear and zFar
            GLU glu = new GLU();
			glu.gluPerspective(45.0f, aspect, 0.1f, 100.0f);
    	}

		protected static void render( GL2 gl2, int width, int height ) {
			gl2.glViewport(0, 0, width, height);
			
		   gl2.glMatrixMode(GL2.GL_MODELVIEW);     // To operate on model-view matrix
    		   
    	   gl2.glLoadIdentity();                 // Reset the model-view matrix
    	   gl2.glTranslatef(0f, 0.0f, -7.0f);  // Move right and into the screen
    	   gl2.glRotatef(angleCube, 1.0f, 1.0f, 1.0f);  // Rotate about (1,1,1)-axis [NEW]
    	 
    	   gl2.glBegin(GL2.GL_QUADS);                // Begin drawing the color cube with 6 quads
    	      // Top face (y = 1.0f)
    	      // Define vertices in counter-clockwise (CCW) order with normal pointing out
    	      gl2.glColor3f(0.0f, 1.0f, 0.0f);     // Green
    	      gl2.glVertex3f( 1.0f, 1.0f, -1.0f);
    	      gl2.glVertex3f(-1.0f, 1.0f, -1.0f);
    	      gl2.glVertex3f(-1.0f, 1.0f,  1.0f);
    	      gl2.glVertex3f( 1.0f, 1.0f,  1.0f);
    	 
    	      // Bottom face (y = -1.0f)
    	      gl2.glColor3f(1.0f, 0.5f, 0.0f);     // Orange
    	      gl2.glVertex3f( 1.0f, -1.0f,  1.0f);
    	      gl2.glVertex3f(-1.0f, -1.0f,  1.0f);
    	      gl2.glVertex3f(-1.0f, -1.0f, -1.0f);
    	      gl2.glVertex3f( 1.0f, -1.0f, -1.0f);
    	 
    	      // Front face  (z = 1.0f)
    	      gl2.glColor3f(1.0f, 0.0f, 0.0f);     // Red
    	      gl2.glVertex3f( 1.0f,  1.0f, 1.0f);
    	      gl2.glVertex3f(-1.0f,  1.0f, 1.0f);
    	      gl2.glVertex3f(-1.0f, -1.0f, 1.0f);
    	      gl2.glVertex3f( 1.0f, -1.0f, 1.0f);
    	 
    	      // Back face (z = -1.0f)
    	      gl2.glColor3f(1.0f, 1.0f, 0.0f);     // Yellow
    	      gl2.glVertex3f( 1.0f, -1.0f, -1.0f);
    	      gl2.glVertex3f(-1.0f, -1.0f, -1.0f);
    	      gl2.glVertex3f(-1.0f,  1.0f, -1.0f);
    	      gl2.glVertex3f( 1.0f,  1.0f, -1.0f);
    	 
    	      // Left face (x = -1.0f)
    	      gl2.glColor3f(0.0f, 0.0f, 1.0f);     // Blue
    	      gl2.glVertex3f(-1.0f,  1.0f,  1.0f);
    	      gl2.glVertex3f(-1.0f,  1.0f, -1.0f);
    	      gl2.glVertex3f(-1.0f, -1.0f, -1.0f);
    	      gl2.glVertex3f(-1.0f, -1.0f,  1.0f);
    	 
    	      // Right face (x = 1.0f)
    	      gl2.glColor3f(1.0f, 0.0f, 1.0f);     // Magenta
    	      gl2.glVertex3f(1.0f,  1.0f, -1.0f);
    	      gl2.glVertex3f(1.0f,  1.0f,  1.0f);
    	      gl2.glVertex3f(1.0f, -1.0f,  1.0f);
    	      gl2.glVertex3f(1.0f, -1.0f, -1.0f);
    	   gl2.glEnd();  // End of drawing color-cube
    	}
    	   
    }
}