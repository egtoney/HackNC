varying vec4 pcolor; // Need to match the name with the desired variable from the vertex program

// This fragment shader just passes the already interpolated fragment color

void main()
{
	gl_FragColor = pcolor; // note that gl_FragColor is a default name for the final fragment color
}