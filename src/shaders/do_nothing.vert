attribute vec4 a_Position;
attribute vec2 a_TexCoordinate;

varying vec4 pcolor; // this is the output variable to the fragment shader

// this shader just pass the vertex position and color along, doesn't actually do anything 
// Note that this means the vertex position is assumed to be already in clip space 
//
void main()
{
	gl_Position = gl_ModelViewProjectionMatrix * a_Position;
	pcolor = vec4(1,0,1,1);
}