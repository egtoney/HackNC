#ifndef LASERDRIVER_h
#define LASERDRIVER_h

#include <wiringPi.h>


class LaserDriver {

	public:
		LaserDriver();
			//Constructor

		void	lSet( int X, int Y );
		void	lOn( bool _on );
		void	lPlot(int X, int Y, int X2, int Y2);
		void	testPins();
	private:
		int 
			D_pin[8],
			WR_pin,
			SEL_pin,
			Laser_pin,


			X_laser,
			Y_laser,
			X_laser_prev,
			Y_laser_prev;

		void	setGalvoXY( char dir );
				//Sets the driver to either
				//	accept a X or Y pos

		void	setDataBus( int pos );
				//Sets the position of the laser bus

		void	writeData();
				//Writes the data to the hardware
};

#endif