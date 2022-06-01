/* ----- Serial to MIDI handler ----- */
(
	SerialPort.devices;
	SerialPort.closeAll;
	~serialPortName = "COM3";
	~serialBaudrate = 9600;
	~serialPort = SerialPort.new(port: ~serialPortName, baudrate: ~serialBaudrate);
	~serialPortCharacters = [ ];

	Routine.new({
		var ascii, whiteNote, blackNote, noteOff;
		~serialPortCharacters = [ ];
		{
			ascii = ~serialPort.read.asAscii;
			if(ascii.isDecDigit, { ~serialPortCharacters = ~serialPortCharacters.add(ascii) });

			switch (ascii)
			{$w} {
				whiteNote = ~serialPortCharacters.collect(_.digit).convertDigits;
				~serialMidiNote = whiteNote;
				~serialMidiMessage = 1;
				~serialPortCharacters = [ ];
			}
			{$b} {
				blackNote= ~serialPortCharacters.collect(_.digit).convertDigits;
				~serialMidiNote = blackNote;
				~serialMidiMessage = 1;
				~serialPortCharacters = [ ];
			}
			{$s} {
				noteOff = ~serialPortCharacters.collect(_.digit).convertDigits;
				~serialMidiNote = noteOff;
				~serialMidiMessage = 0;
				~serialPortCharacters = [ ];
			};

			0.03.wait;
		}.loop();
	}).play();
);