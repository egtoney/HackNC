#include "LaserDriver.h"
#include <wiringPi.h>


void LaserDriver::testPins() {
	digitalWrite( Laser_pin, LOW );
	delay(500);
	digitalWrite( Laser_pin, HIGH );
	delay(500);
}

void LaserDriver::lSet( int X, int Y ) {
	setGalvoXY('X');
	setDataBus( X );
	
	writeData();

	setGalvoXY('Y');
	setDataBus( Y );

	writeData();
}

LaserDriver::LaserDriver() {

	wiringPiSetup();

	D_pin[0] = 8;
	D_pin[1] = 9;
	D_pin[2] = 7;
	D_pin[3] = 15;
	D_pin[4] = 16;
	D_pin[5] = 0;
	D_pin[6] = 1;
	D_pin[7] = 2;

	WR_pin = 4;
	SEL_pin = 3;

	Laser_pin = 10;

	for (int i = 0; i<8; i++)
		pinMode(D_pin[i], OUTPUT);

	pinMode( WR_pin, OUTPUT );
	pinMode( SEL_pin, OUTPUT );
	pinMode( Laser_pin, OUTPUT );
}

void LaserDriver::setGalvoXY( char dir ) {

	switch (dir) {
		case 'X':
			digitalWrite( SEL_pin, LOW );
			break;
		default:
			digitalWrite( SEL_pin, HIGH );
			break;
	}
}

void LaserDriver::setDataBus( int pos ) {
	int temp;

	for (int i=7; i>=0; i--) {
		temp = pos >> i;

		if ( temp & 1 )
			digitalWrite( D_pin[i], HIGH );
		else
			digitalWrite( D_pin[i], LOW );
	}
}

void LaserDriver::writeData() {
	digitalWrite( WR_pin, LOW );
	//delay(1);
	digitalWrite( WR_pin, HIGH );
}
