var voiceChannelsGroup, voiceChannels, synth, pitchFollower, outputGroup, outputMixer, oscManager, window, windowWidth, windowHeight, titleWidth, titleHeight, knobWidth, knobHeight, sliderWidth, sliderHeight, margin, voiceSectionWidth, voiceSectionYOffset, voiceSectionMargin, currentXPos, currentYPos, xOffset, masterTitle, pitchShifterTitle, button, buttonWidth, buttonHeight, knob, backgroundImage, dropMenuWidth, dropMenuHeight, titleBottomPadding;

Server.killAll;
s.waitForBoot({

	/* ----- Environment settings ----- */
	(
		/* ----- Global settings ----- */
		~voiceNumber = 4;
		~windowSize = 0.075;
		~minDelayTime = s.options.blockSize/s.sampleRate;
		~maxDelayTime = 2.0;
		~fontName = "Arial Black";
		/* ----- Audio buses ----- */
		~inputAudioBus = Bus.audio(s, 1);
		~delayedVoiceBuses = Array.fill(~voiceNumber, {arg i; Bus.audio(s, 1)});
		~pitchShiftedVoiceBuses = Array.fill(~voiceNumber, {arg i; Bus.audio(s, 1)});
		/* ----- Control buses ----- */
		~serialMidiMessageControlBus = Bus.control(s, 1);
		~serialMidiNoteControlBus = Bus.control(s, 1);
		~oscControlBus = Bus.control(s, 2 + ~voiceNumber);
		~pitchDetectionControlBus = Bus.control(s, 1);
		~formantRatioControlBus = Bus.control(s, 1);
		~grainsPeriodControlBus = Bus.control(s, 1);
		~timeDispersionControlBus = Bus.control(s, 1);
		~pitchRatioControlBuses = Array.fill(~voiceNumber, {arg i; Bus.control(s, 1)});
		~stereoPanControlBuses = Array.fill(~voiceNumber, {arg i; Bus.control(s, 1)});
		~modeSelectionBuses = Array.fill(~voiceNumber, {arg i; Bus.control(s, 1)});
		~keyControlBus = Bus.control(s, 1);
		~scaleControlBus = Bus.control(s, 1);
		~rootIndexControlBus = Bus.control(s, 1);
		~voiceIntervalBusses = Array.fill(~voiceNumber, {arg i; Bus.control(s, 1)});
		~octaveUpDownBuses = Array.fill(~voiceNumber, {arg i; Bus.control(s, 1)});
		~octaveNumberBuses = Array.fill(~voiceNumber, {arg i; Bus.control(s, 1)});
		/* ----- Keys and intervals ----- */
		~currentKey = 0;
		~currentScale = 0;
		~chromaticFreqs = [261.63, 277.18, 293.66, 311.13, 329.63, 349.23, 369.99, 392.00, 415.30, 440.00, 466.16, 493.88];
		~keys = ['c', 'cs', 'd', 'ds', 'e', 'f', 'fs', 'g', 'gs', 'a', 'as', 'b'];
		~scales = [
			[2, 2, 1, 2, 2, 2, 1], // Natural Major
			[2, 1, 2, 2, 1, 2, 2], // Natural Minor
			[2, 1, 2, 2, 2, 2, 1], // Melodic Minor
			[2, 1, 2, 2, 1, 2, 1], // Harmonic Minor
			[1, 3, 1, 2, 1, 3, 1], // Double Harmonic
			[2, 1, 2, 2, 2, 1, 2], // Dorian
			[1, 2, 2, 2, 1, 2, 2], // Phrygian
			[2, 2, 2, 1, 2, 2, 1], // Lydian
			[2, 2, 1, 2, 2, 1, 2], // Mixolydian
			[1, 2, 2, 1, 2, 2, 2], // Locrian
			[1, 2, 2, 2, 2, 2, 1], // Neapolitan Major
			[1, 2, 2, 2, 1, 3, 1], // Neapolitan Minor
			[2, 1, 2, 1, 2, 1, 2], // Romanian Major
			[2, 1, 3, 1, 1, 2, 2], // Romanian Minor
			[2, 1, 3, 1, 1, 3, 1], // Hungarian
			[1, 3, 1, 1, 3, 1, 2], // Oriental
			[1, 3, 2, 2, 2, 1, 1]  // Enigmatic
			// TODO - Add support for non-hectatonic scales
			// Problem: Select returns the greatest size in a multi-dim array
			/*
			[2, 2, 3, 2, 3], // Major Pentatonic
			[2, 3, 2, 3, 2], // Suspended Pentatonic (Egyptian)
			[3, 2, 3, 2, 2], // Blues Minor Pentatonic (Man Gong)
			[2, 3, 2, 2, 3], // Blues Major Pentatonic (Ritusen)
			[3, 2, 2, 3, 2], // Minor Pentatonic
			[4, 2, 1, 4, 1], // Eastern Pentatonic
			[2, 1, 4, 1, 4], // Suling Pentatonic
			[1, 2, 1, 2, 1, 2, 1, 2], // Whole-Step/Half-Step Diminished Scale (Octatonic)
			[2, 1, 2, 1, 2, 1, 2, 1], // Half-Step/Whole-Step Diminished Scale (Octatonic)
			[2, 2, 2, 2, 2, 2], // Whole-Tone Scale (Exatonic)
			[1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1] // Chromatic Scale
			*/
		];
		~intervalsMask = [
			[0, 0, 0, 0, 0, 0, 0], // Unison
			[1, 0, 0, 0, 0, 0, 0], // Second
			[1, 1, 0, 0, 0, 0, 0], // Third
			[1, 1, 1, 0, 0, 0, 0], // Fourth
			[1, 1, 1, 1, 0, 0, 0], // FIfth
			[1, 1, 1, 1, 1, 0, 0], // Sixth
			[1, 1, 1, 1, 1, 1, 0], // Seventh
		];
		/* ----- Buffers ----- */
		// ~inputBuffer = Buffer.read(s, thisProcess.nowExecutingPath.dirname +/+ "loops/Gm Let Me Love You.wav");
		/* ----- Serial ----- */
		~serialMidiNote = 50;
		~serialMidiMessage = 1;
		/* ----- OSC ----- */
		~oscNetAddrProcessing = NetAddr.new("127.0.0.1", 7777);
		~oscNetAddrTouchOSC = NetAddr.new("192.168.43.183", 9000);
		~keyNum = 0;
		~keysLabels = ['C', 'C#', 'D', 'D#', 'E', 'F', 'F#', 'G', 'G#', 'A', 'A#', 'B'];
		~scaleNum = 0;
		~scalesLabels = [
				'Natural Major',
				'Natural Minor',
				'Melodic Minor',
				'Harmonic Minor',
				'Double Harmonic',
				'Dorian',
				'Phrygian',
				'Lydian',
				'Mixolydian',
				'Locrian',
				'Neapolitan Major',
				'Neapolitan Minor',
				'Romanian Major',
				'Romanian Minor',
				'Hungarian',
				'Oriental',
				'Enigmatic'
			];
		~octavesNum = 0;
		~upDownNum = 0;
		~upDownLabels = ['Up', 'Down'];
		~feedbackModeNum = 0;
		~feedbackModesLabels = [ 'Normal Feedback', 'Pitch Feedback', 'Cross Feedback' ];
	);
	/* ----- Sound Input -----
	Reads the audio from the hardware input
	an writes it to inputAudioBus */
	(
		a = SynthDef.new(\soundIn, {
			Out.ar(
				bus: ~inputAudioBus,
				channelsArray: SoundIn.ar(0);
				/* channelsArray: PlayBuf.ar(
					numChannels: 1,
					bufnum: ~inputBuffer,
					rate: BufRateScale.kr(~inputBuffer),
					trigger: 1.0,
					startPos: 0.0,
					loop: 1.0,
					doneAction: Done.freeSelf
				) */
			);
		}).add;
	);

	/* ----- Wavatable Synth ----- */
	(
		c = SynthDef.new(\synth, { arg sinTableGain = 1, chebTableGain = 0.12;
			var midiNote, gate, sineSignal, chebSignal, soundSin, soundCheb, sinBuffer, chebBuffer, synthSignal;

			midiNote = In.kr(~serialMidiNoteControlBus, 1);
			gate = In.kr(~serialMidiMessageControlBus, 1); // (0: NoteOff - 1: NoteOn)

			/* ----- Wavatables ----- */
			// Sine Wavetable
			sineSignal = Signal.sineFill(
				size: 1024,
				amplitudes: [1, 1/4, 1/6, 1/2, 4/5, 1/6],
				phases: 0!6
			);
			sinBuffer = LocalBuf.new(numFrames: 2048, numChannels: 1);
			sinBuffer.set(sineSignal.asWavetable);
			soundSin = Osc.ar(
				bufnum: sinBuffer,
				freq: midiNote.midicps,
				mul: sinTableGain
			);
			// Chebyshev Wavetable
			chebSignal = Signal.chebyFill(
				size: 1024,
				amplitudes: [1, 1/2, 1/6, 1/2, 1/8, 1/4],
				normalize: true
			);

			chebBuffer = LocalBuf.new(numFrames: 2048, numChannels: 1);
			chebBuffer.set(chebSignal.asWavetable);
			soundCheb = Osc.ar(
				bufnum: chebBuffer,
				freq: midiNote.midicps,
				mul: chebTableGain
			);

			synthSignal = Mix.ar([soundSin, soundCheb]) * gate;
			Out.ar(~inputAudioBus, synthSignal);
		}).add;
	);

	/* ----- Pitch Detection -----
	Detects the pitch of the input signal. */
	(
		d = SynthDef.new(\pitchDetector, {
			var input, freq, hasFreq, outputSelector;
			input = In.ar(~inputAudioBus, 1);
			# freq, hasFreq = Pitch.kr(
				in: input,
				initFreq: 0,
				minFreq: 60.0,
				maxFreq: 4000.0,
				execFreq: 100.0,
				maxBinsPerOctave: 1024,
				median: 7,
				ampThreshold: 0.02,
				peakThreshold: 0.7,
				downSample: 1,
				clar: 0
			);
			/*# freq, hasFreq = Tartini.kr(
				in: input,
				threshold: 0.93,
				n: 2048,
				k: 0,
				overlap: 1024,
				smallCutoff: 0
			);*/

			Out.kr(~pitchDetectionControlBus, freq);
		}).add
	);

	/* ----- Keys and Intervals Manager -----
	Automatically sets the values for the voices's pitch ratios.
	These will be computed based on the detected input frequency
	and accordingly to the specified key, scale and voice intervals. */
	(
		e = SynthDef.new(\pitchRatioManager, { arg channelIndex;
			var detectedFreq, key, scale, referenceFreqIndex, referenceFreq, chromaticFreqsBuffer, freqsLocBuffer, currentPos, pitchRatio, voiceInterval, rootIndex, firstInterval, rotatedScale, intervalMask, maskedScale, octaveNumber, octaveUpDown;

			// Read current key and scale
			key = In.kr(~keyControlBus, 1);
			scale = Select.kr(In.kr(~scaleControlBus, 1), ~scales);

			// Compute ratio for current voice
			// Get the index of the root note
			rootIndex = In.kr(~rootIndexControlBus, 1);
			// Rotate the scale array so the root note is in the first position
			rotatedScale = Select.kr(((0..(scale.size - 1)) + rootIndex).wrap(0, scale.size), scale);
			// Get the interval selected for the voice
			voiceInterval = In.kr(Select.kr(channelIndex, ~voiceIntervalBusses), 1);
			// Use the interval as index to get the correspondent mask
			intervalMask = Select.kr(voiceInterval, ~intervalsMask);
			// Apply the mask
			maskedScale = intervalMask * rotatedScale;
			// Sum all the elements in the masked array to get the pitch ratio (in semitones)
			pitchRatio = maskedScale.sum;
			// Apply selected octaves
			octaveNumber = In.kr(Select.kr(channelIndex, ~octaveNumberBuses), 1);
			pitchRatio = pitchRatio + (12*octaveNumber);
			// Choose Up/Down octave (0:Up - 1:Down)
			octaveUpDown = In.kr(Select.kr(channelIndex, ~octaveUpDownBuses), 1);
			pitchRatio = Select.kr(octaveUpDown, [pitchRatio, -1*pitchRatio]);

			// DEBUG
			// rootIndex.poll;
			//(scale.size - 1).poll; -> 6
			//(0..(scale.size - 1)).poll; -> [0, 1, 2, 3, 4, 5, 6]
			//((0..(scale.size - 1)) + rootIndex).poll;
			// ((0..(scale.size - 1)) + rootIndex).wrap(0, scale.size).poll;
			//rootIndex.poll((HPZ1.kr(rootIndex).abs > 0) + Impulse.kr(0));
			//rotatedScale.poll;
			//voiceInterval.poll((HPZ1.kr(voiceInterval).abs > 0) + Impulse.kr(0));
			//intervalMask.poll((HPZ1.kr(voiceInterval).abs > 0) + Impulse.kr(0));
			//maskedScale.poll((HPZ1.kr(voiceInterval).abs > 0) + Impulse.kr(0));
			//pitchRatio.poll((HPZ1.kr(voiceInterval).abs > 0) + Impulse.kr(0));

			Out.kr(Select.kr(channelIndex, ~pitchRatioControlBuses), pitchRatio);
		}).add;
	);

	/* ----- Pitch Shifter -----
	Pitch shift the input signal based on the selected mode. */
	(
		f = SynthDef.new(\pitchShifter, { arg channelIndex, gain = 0.0;
			var input, detectedFreq, delayedSignal, pitchShiftInput, isPitchShiftFeedbackMode, pitchRatio, formantRatio, grainsPeriod, timeDispersion, pitchShiftedSignal, isCrossFeedback, selectedMode;

			input = In.ar(~inputAudioBus, 1);
			detectedFreq = In.kr(~pitchDetectionControlBus, 1);
			delayedSignal = InFeedback.ar(Select.kr(channelIndex, ~delayedVoiceBuses), 1);
			selectedMode = In.kr(Select.kr(channelIndex, ~modeSelectionBuses), 1);
			pitchShiftInput = Select.ar(selectedMode, [input, Mix.new([input, delayedSignal]), input]);
			formantRatio = In.kr(~formantRatioControlBus, 1);
			grainsPeriod = In.kr(~grainsPeriodControlBus, 1);
			timeDispersion = In.kr(~timeDispersionControlBus, 1);
			pitchRatio = In.kr(Select.kr(channelIndex, ~pitchRatioControlBuses), 1);
			pitchShiftedSignal = PitchShiftPA.ar(
				in: pitchShiftInput,
				freq: detectedFreq,
				pitchRatio: (2.pow(1/12)).pow(pitchRatio),
				formantRatio: formantRatio,
				minFreq: 10,
				maxFormantRatio: 10,
				grainsPeriod: grainsPeriod,
				timeDispersion: timeDispersion
			);
			/*pitchShiftedSignal = PitchShift.ar(
				in: pitchShiftInput,
				windowSize: 0.075,
				pitchRatio: (2.pow(1/12)).pow(pitchRatio),
				pitchDispersion: 0.0,
				timeDispersion: 0.075,
				mul: 1.0
			);*/

			Out.ar(Select.kr(channelIndex, ~pitchShiftedVoiceBuses), gain*pitchShiftedSignal);
		}).add;
	);

	/* ----- Feedback Delay Line -----
	A feedback delay line that uses the FbNode class of the Feedback Quark: the input feedback signal
	to the node changes accordingly to the selected mode. */
	(
		g = SynthDef.new(\feedbackDelayLine, { arg channelIndex, delayTime = ~minDelayTime, feedbackAmount = 0.0;
			var input, feedbackNode, delayedSignal, feedbackSignal, pitchShiftedSignal, selectedMode, channelFeedbackBus, env, gen;

			input = In.ar(~inputAudioBus, 1);
			feedbackNode = FbNode(numChannels: 1, maxdelaytime: ~maxDelayTime, interpolation: 2);
			delayedSignal = feedbackNode.delay(delayTime);
			pitchShiftedSignal = In.ar(Select.kr(channelIndex, ~pitchShiftedVoiceBuses), 1);

			selectedMode = In.kr(Select.kr(channelIndex, ~modeSelectionBuses), 1);

			delayedSignal = Select.ar(selectedMode, [
				delayedSignal,
				delayedSignal,
				Mix.new([delayedSignal] ++ ~pitchShiftedVoiceBuses.collect({
					arg pitchShiftedVoiceBus, i;
					if (pitchShiftedVoiceBus.index != channelIndex,
						{ feedbackAmount*InFeedback.ar(pitchShiftedVoiceBus, 1) },
						{ 0.0 }
					);
				}));
			]);

			feedbackSignal = Select.ar(selectedMode, [
				feedbackAmount*Mix.new([pitchShiftedSignal, delayedSignal]),
				Mix.new([feedbackAmount*pitchShiftedSignal]),
				feedbackAmount*Mix.new([pitchShiftedSignal, delayedSignal]),
			]);

			feedbackNode.write(feedbackSignal);
			Out.ar(Select.kr(channelIndex, ~delayedVoiceBuses), delayedSignal);
		}).add;
	);

	/* ----- Mixer -----
	Mixes the mono input and the pitch shifted voices to a stereo ouput. */
	(
		h = SynthDef.new(\mixer, { arg master = 1, wet = 0.5, reverbMix = 0.5, roomDimension = 0.5, reverbHighDamp = 0.5, lpfCutoff = 4000, hpfCutoff = 100, lfoFreq = 10, lfoAmp = 0.075, lpfResonance = 0, hpfResonance = 0, adsrA = 0.1, adsrD = 0.05, adsrS = 0.5, adsrR = 1.0;
			var monoInput, stereoInput, stereoOutput, voiceStereoSignals, selectedMode, env, gen, lfo;
			monoInput = In.ar(~inputAudioBus, 1);
			stereoInput = Pan2.ar((1 - wet) * monoInput, 0.0);
			voiceStereoSignals = ~pitchShiftedVoiceBuses.collect({
				arg pitchShiftedVoiceBus, i;
				var pitchShiftedVoice, delayedSignal, stereoPan, voiceOutput;
				stereoPan = In.kr(~stereoPanControlBuses[i], 1);
				pitchShiftedVoice = In.ar(pitchShiftedVoiceBus, 1);
				delayedSignal = In.ar(~delayedVoiceBuses[i], 1);
				selectedMode = In.kr(Select.kr(i, ~modeSelectionBuses), 1);
				voiceOutput = Select.ar(selectedMode, [
					Mix.new([pitchShiftedVoice, delayedSignal]),
					pitchShiftedVoice,
					Mix.new([pitchShiftedVoice, delayedSignal])
				]);
				Pan2.ar(wet * voiceOutput, stereoPan);
			});
			stereoOutput = Mix.new(stereoInput ++ voiceStereoSignals);

			/* ----- Envelope Generator ----- */
			env = Env.adsr(
				attackTime: adsrA,
				decayTime: adsrD,
				sustainLevel: adsrS,
				releaseTime: adsrR,
				peakLevel: 1.0,
				curve: -4.0,
				bias: 0.0
			);
			gen = EnvGen.kr(env);

			/* ----- LFO ----- */
			lfo = SinOsc.kr(freq: lfoFreq, phase: 0, mul: lfoAmp, add: 1);

			/* ----- Filters ----- */
			stereoOutput = RLPF.ar(stereoOutput, lpfCutoff, lpfResonance);
			stereoOutput = RHPF.ar(stereoOutput, hpfCutoff, hpfResonance);

			/* ----- Reverb ----- */
			stereoOutput = FreeVerb2.ar( // FreeVerb2 - true stereo UGen
				in: stereoOutput[0],     // left channel
				in2: stereoOutput[1],    // right channel
				mix: reverbMix,          // dry[0]/wet[1]
				room: roomDimension,     // room size [0..1]
				damp: reverbHighDamp     // high frequncies rol-off [0..1]
			);
			Out.ar(0,stereoOutput * master * gen * lfo);
		}).add;
	);

	/* ----- OSC Manager -----
	Set the values for the OSCbus and send the trigger */
	(
		i = SynthDef.new(\oscManager, {
			var rootNote, messageType, oscBusArray;
			rootNote = In.kr(~serialMidiNoteControlBus, 1);
			messageType = In.kr(~serialMidiMessageControlBus, 1);
			oscBusArray = [messageType, rootNote];
			~pitchRatioControlBuses.do({arg pitchRatioBus, i;
				var pitchRatio;
				pitchRatio = In.kr(pitchRatioBus, 1);
				oscBusArray = oscBusArray ++ [rootNote + pitchRatio];
			});

			// Set OSC control bus multi-channel bus and send trigger to the client
			Out.kr(~oscControlBus, oscBusArray);
			SendReply.kr(Impulse.kr(3), '/oscAnswer', In.kr(~oscControlBus, ~oscControlBus.numChannels));
		}).add;
	);

	/* ----- Synths and GUI ----- */
	(
		AppClock.sched(0.05, {
			/* ----- Synths ----- */
			// x = Synth(\soundIn);
			synth = Synth(\synth);
			pitchFollower = Synth.after(synth, \pitchDetector);
			voiceChannelsGroup = ParGroup.after(pitchFollower);
			voiceChannels = Array.fill(~voiceNumber, {
				arg i;
				var pitchRatioManager, pitchShifter, feedbackDelayLine;
				pitchRatioManager = Synth.head(voiceChannelsGroup, \pitchRatioManager, [\channelIndex, i]);
				pitchShifter = Synth.after(pitchRatioManager, \pitchShifter, [\channelIndex, i]);
				feedbackDelayLine = Synth.after(pitchShifter, \feedbackDelayLine, [\channelIndex, i]);
				[pitchRatioManager, pitchShifter, feedbackDelayLine];
			});
			outputGroup = ParGroup.after(voiceChannelsGroup);
			outputMixer = Synth.head(outputGroup, \mixer);
			oscManager = Synth.after(outputMixer, \oscManager);

			/* ----- Start setting bus values with incoming serial messages ----- */
			Routine.new({
				{
					var currentPos, currentScaleFreqs;
					~serialMidiNoteControlBus.set(~serialMidiNote);
					~serialMidiMessageControlBus.set(~serialMidiMessage);
					currentPos = ~currentKey;
					currentScaleFreqs = Array.fill(~scales[~currentScale].size, {arg i;
						var note;
						// pull the freq for the current note
						note = ~chromaticFreqs.wrapAt(currentPos);
						// move to the next note for next time
						currentPos = currentPos + ~scales[~currentScale].at(i);
						note;
					});
					~rootIndexControlBus.set(currentScaleFreqs.detectIndex({
							arg item, i;
							item == ~chromaticFreqs.at(~serialMidiNote % 12);
						})
					);

					0.03.wait;
				}.loop;
			}).play;

			/* ----- Send OSC messages ----- */
			o = OSCFunc({ arg msg, time;
				// Message contains the OSC array polled from the trigger
				var data = msg[3..];
				~oscNetAddrProcessing.sendMsg("/harmmmlonizer", data[0], data[1], data[2..]);
			}, '/oscAnswer', s.addr);

			/* ----- GUI ----- */
			Window.closeAll;
			windowWidth = 1625;
			windowHeight = 860;
			titleWidth = 200;
			titleHeight = 70;
			knobWidth = 125;
			knobHeight = knobWidth;
			sliderWidth = 300;
			sliderHeight = 50;
			buttonWidth = 120;
			buttonHeight = 40;
			margin = 5@5;
			titleBottomPadding = 15;

			window = Window(
				name: "HarMMMLonizer",
				bounds: Rect(100, 100, windowWidth, windowHeight),
				resizable: false,
				border: true,
				scroll: false
			);
			try {
				backgroundImage = Image.open(thisProcess.nowExecutingPath.dirname +/+ "assets/images/background.png");
			} {
				backgroundImage = Image.color(windowWidth, windowHeight, Color.new255(140, 175, 189));
			};
			window.view.backgroundImage_(backgroundImage);

			/* ----- Master Section ----- */
			currentYPos = 50;
			/* ----- Input Meter ----- */
			currentXPos = 175;
			ServerMeterView.new(
				aserver: s,
				parent: window,
				leftUp: currentXPos@currentYPos,
				numIns: 1,
				numOuts: 0
			);
			/* ----- Title ----- */
			currentXPos = (730 - titleWidth)/2;
			masterTitle = StaticText(
				parent: window,
				bounds: Rect(currentXPos, currentYPos, titleWidth, titleHeight)
			);
			masterTitle.string = "Master";
			masterTitle.font = Font("~fontName", 30);
			masterTitle.align = \center;
			currentYPos = currentYPos + titleHeight + titleBottomPadding;
			/* ----- Master Gain Knob ----- */
			currentXPos = 730/2 - knobWidth;
			knob = EZKnob(
				parent: window,
				bounds: Rect(currentXPos, currentYPos, knobWidth, knobHeight),
				label: "Gain",
				controlSpec: ControlSpec.new(minval: 0.0, maxval: 2.0, warp: \lin),
				action: {arg thisKnob; outputMixer.set(\master, thisKnob.value)},
				initVal: 1.0,
				initAction: false,
				labelWidth: 60,
				// knobSize: an instance of Point,
				unitWidth: 0,
				labelHeight: 20,
				layout: \vert2,
				// gap: an instance of Point,
				margin: margin
			);
			knob.font = Font(~fontName, 11);
			/* ----- Dry/Wet Knob ----- */
			currentXPos = currentXPos + knobWidth;
			knob = EZKnob(
				parent: window,
				bounds: Rect(currentXPos, currentYPos, knobWidth, knobHeight),
				label: "Dry/Wet",
				controlSpec: ControlSpec.new(minval: 0.0, maxval: 1.0, warp: \lin),
				action: {arg thisKnob; outputMixer.set(\wet, thisKnob.value)},
				initVal: 0.5,
				initAction: false,
				labelWidth: 60,
				// knobSize: an instance of Point,
				unitWidth: 0,
				labelHeight: 20,
				layout: \vert2,
				// gap: an instance of Point,
				margin: margin
			);
			knob.font = Font(~fontName, 11);
			/* ----- Output Meter ----- */
			currentXPos = currentXPos + knobWidth;
			currentYPos = 50;
			ServerMeterView.new(
				aserver: s,
				parent: window,
				leftUp: currentXPos@currentYPos,
				numIns: 0,
				numOuts: 2
			);

			/* ----- Scale and Mode Section ----- */
		    /* ----- Title ----- */
			currentXPos = (1625 - titleWidth)/2;
			dropMenuWidth = 300;
			dropMenuHeight = 50;
			masterTitle = StaticText(
				parent: window,
				bounds: Rect(currentXPos, currentYPos, titleWidth, titleHeight)
			);
			masterTitle.string = "Scale Selection";
			masterTitle.font = Font("~fontName", 30);
			masterTitle.align = \center;
			currentYPos = currentYPos + titleHeight + titleBottomPadding;
			currentXPos = (1625 - (2*titleWidth))/2;

			/* ----- Key Selection Menu ----- */
			EZPopUpMenu.new(
				parentView: window,
				bounds: Rect(currentXPos, currentYPos, dropMenuWidth, dropMenuHeight),
				label: "Key: ",
				items: ['C', 'C#', 'D', 'D#', 'E', 'F', 'F#', 'G', 'G#', 'A', 'A#', 'B'],
				globalAction: {arg thisMenu;
					var currentPos, currentScaleFreqs;
					currentPos = thisMenu.value;
					~keyControlBus.set(thisMenu.value);
					~currentKey = thisMenu.value;
					currentScaleFreqs = Array.fill(~scales[~currentScale].size, {arg i;
						var note;
						// pull the freq for the current note
						note = ~chromaticFreqs.wrapAt(currentPos);
						// move to the next note for next time
						currentPos = currentPos + ~scales[~currentScale].at(i);
						note;
					});
					~rootIndexControlBus.set(currentScaleFreqs.detectIndex({
							arg item, i;
							item == ~chromaticFreqs.at(~serialMidiNote % 12);
						})
					);
				},
				initVal: 0,
				initAction: true,
				labelWidth: 120,
				labelHeight: 60,
				layout: \horz,
				// gap: an instance of Point,
				// margin: margin
			);

			/* ----- Scale Selection Menu ----- */
			EZPopUpMenu.new (
				parentView: window,
				bounds: Rect(currentXPos, currentYPos + 65, dropMenuWidth, dropMenuHeight),
				label: "Scale: ",
				items: [
					'Natural Major',
					'Natural Minor',
					'Melodic Minor',
					'Harmonic Minor',
					'Double Harmonic',
					'Dorian',
					'Phrygian',
					'Lydian',
					'Mixolydian',
					'Locrian',
					'Neapolitan Major',
					'Neapolitan Minor',
					'Romanian Major',
					'Romanian Minor',
					'Hungarian',
					'Oriental',
					'Enigmatic'
				],
				globalAction: {arg thisMenu;
					var currentPos, currentScaleFreqs;
					currentPos = ~currentKey;
					~scaleControlBus.set(thisMenu.value);
					~currentScale = thisMenu.value;
					currentScaleFreqs = Array.fill(~scales[thisMenu.value].size, {arg i;
						var note;
						// pull the freq for the current note
						note = ~chromaticFreqs.wrapAt(currentPos);
						// move to the next note for next time
						currentPos = currentPos + ~scales[thisMenu.value].at(i);
						note;
					});
					~rootIndexControlBus.set(currentScaleFreqs.detectIndex({
							arg item, i;
							item == ~chromaticFreqs.at(~serialMidiNote % 12);
						})
					);
				},
				initVal: 0,
				initAction: true,
				labelWidth: 120,
				labelHeight: 20,
				layout: \horz,
				// gap: an instance of Point,
				// margin: margin
			);

			/* ----- Pitch Shifter Section ----- */
			currentXPos = (2400 - titleWidth)/2;
			currentYPos = 50;
			pitchShifterTitle = StaticText(
				parent: window,
				bounds: Rect(currentXPos + 50, currentYPos, titleWidth, titleHeight)
			);
			pitchShifterTitle.string = "Pitch Shifter";
			pitchShifterTitle.font = Font("~fontName", 30);
			pitchShifterTitle.align = \center;
			currentYPos = currentYPos + titleHeight + titleBottomPadding;
			currentXPos = currentXPos - knobWidth - 30;
			/* ----- Formant Ratio Knob ----- */
			currentXPos = currentXPos + knobWidth;
			knob = EZKnob(
				parent: window,
				bounds: Rect(currentXPos, currentYPos, knobWidth, knobHeight),
				label: "Formant Ratio",
				controlSpec: ControlSpec.new(minval: nil, maxval: 4, warp: \lin, step: 0.1),
				action: {arg thisKnob; ~formantRatioControlBus.set(thisKnob.value)},
				initVal: 1.0,
				initAction: true,
				labelWidth: 60,
				// knobSize: an instance of Point,
				unitWidth: 0,
				labelHeight: 20,
				layout: \vert2,
				// gap: an instance of Point,
				margin: margin
			);
			knob.font = Font(~fontName, 11);
			/* ----- Grains Period Knob ----- */
			currentXPos = currentXPos + knobWidth;
			knob = EZKnob(
				parent: window,
				bounds: Rect(currentXPos, currentYPos, knobWidth, knobHeight),
				label: "Grains Period",
				controlSpec: ControlSpec.new(minval: 2, maxval: 8, warp: \lin, step: 2),
				action: {arg thisKnob; ~grainsPeriodControlBus.set(thisKnob.value)},
				initVal: 2,
				initAction: true,
				labelWidth: 60,
				// knobSize: an instance of Point,
				unitWidth: 0,
				labelHeight: 20,
				layout: \vert2,
				// gap: an instance of Point,
				margin: margin
			);
			knob.font = Font(~fontName, 11);
			/* ----- Time Dispersion Knob ----- */
			currentXPos = currentXPos + knobWidth;
			knob = EZKnob(
				parent: window,
				bounds: Rect(currentXPos, currentYPos, knobWidth, knobHeight),
				label: "Time Dispersion",
				controlSpec: ControlSpec.new(minval: nil, maxval: 1.0, warp: \lin, step: 0.01),
				action: {arg thisKnob; ~timeDispersionControlBus.set(thisKnob.value)},
				initVal: nil,
				initAction: true,
				labelWidth: 60,
				// knobSize: an instance of Point,
				unitWidth: 0,
				labelHeight: 20,
				layout: \vert2,
				// gap: an instance of Point,
				margin: margin
			);
			knob.font = Font(~fontName, 11);

			/* ----- Voice Channels Section ----- */
			voiceChannels.do({ arg voiceChannel, index;

				var title, pitchRatioManager, pitchShifter, feedbackDelayLine, pitchfbModeCheckbox, crossfbModeCheckbox;
				xOffset = 55 + (400*index);

				pitchRatioManager = voiceChannel[0];
				pitchShifter = voiceChannel[1];
				feedbackDelayLine = voiceChannel[2];

				/* ----- Title ----- */
				currentXPos = xOffset + ((sliderWidth - titleWidth)/2);
				currentYPos = 280;
				title = StaticText(
					parent: window,
					bounds: Rect(currentXPos, currentYPos, titleWidth, titleHeight)
				);
				title.string = "Voice " ++ (index + 1);
				title.font = Font("~fontName", 30);
				title.align = \center;

				/* ----- First Line ----- */
				currentYPos = currentYPos + titleHeight;
				/* ----- Amount Knob ----- */
				currentXPos = xOffset + (sliderWidth/2 - knobWidth);
				knob = EZKnob(
					parent: window,
					bounds: Rect(currentXPos, currentYPos, knobWidth, knobHeight),
					label: "Gain",
					controlSpec: ControlSpec.new(minval: 0.0, maxval: 2.0, warp: \lin),
					action: {arg thisKnob; pitchShifter.set(\gain, thisKnob.value)},
					initVal: 0.0,
					initAction: false,
					labelWidth: 60,
					// knobSize: an instance of Point,
					unitWidth: 0,
					labelHeight: 20,
					layout: \vert2,
					// gap: an instance of Point,
					margin: margin
				);
				knob.font = Font(~fontName, 11);
				/* ----- Stereo Pan Knob ----- */
				currentXPos = currentXPos + knobWidth;
				knob = EZKnob(
					parent: window,
					bounds: Rect(currentXPos, currentYPos, knobWidth, knobHeight),
					label: "Pan",
					controlSpec: ControlSpec.new(minval: -1.0, maxval: 1.0, warp: \lin),
					action: {arg thisKnob; ~stereoPanControlBuses[index].set(thisKnob.value)},
					initVal: 0.0,
					initAction: true,
					labelWidth: 60,
					// knobSize: an instance of Point,
					unitWidth: 0,
					labelHeight: 20,
					layout: \vert2,
					// gap: an instance of Point,
					margin: margin
				);
				knob.font = Font(~fontName, 11);

				/* ----- Third Line ----- */
				currentYPos = currentYPos + knobHeight + 30;
				/* ----- Voice Interval Slider ----- */
				currentXPos = xOffset;
				knob = EZSlider(
					parent: window,
					bounds: Rect(currentXPos, currentYPos, sliderWidth, sliderHeight),
					label: "Voice Interval",
					controlSpec: ControlSpec(minval: 1, maxval: 7, warp: \lin, step: 1),
					action: {arg thisSlider; ~voiceIntervalBusses[index].set(thisSlider.value - 1); },
					initVal: 0,
					initAction: true,
					labelWidth: 60,
					numberWidth: 60,
					// knobSize: an instance of Point,
					unitWidth: 30,
					labelHeight: 20,
					layout: \line2,
					// gap: an instance of Point,
					margin: margin
				);
				knob.font = Font(~fontName, 11);

				/* ----- Fourth Line ----- */
				currentYPos = currentYPos + sliderHeight + 7;
				/* ----- Ocatve Label ----- */
				currentXPos = xOffset + 7;
				title = StaticText(
					parent: window,
					bounds: Rect(currentXPos, currentYPos, 70, 30)
				);
				title.string = "Octave";
				title.font = Font(~fontName, 11);
				title.align = \left;
				/* ----- Octave Up/Down ----- */
				currentXPos = currentXPos + 70;
				button = Button.new(
					parent: window,
					bounds: Rect(currentXPos, currentYPos, 30, 30));
				button.states = [["+", Color.black], ["-", Color.black]];
				button.action = ({ arg me;
					~octaveUpDownBuses[index].set(me.value);
				});
				button.font = Font(~fontName, 11);
				/* ----- Octave Number ----- */
				currentXPos = currentXPos + 40;
				button = Button.new(
					parent: window,
					bounds: Rect(currentXPos, currentYPos, 30, 30));
				button.states = [["0", Color.black], ["1", Color.black], ["2", Color.black]];
				button.action = ({ arg me;
					~octaveNumberBuses[index].set(me.value);
				});
				button.font = Font(~fontName, 11);

				/* ----- Fifth Line ----- */
				currentYPos = currentYPos + 60;
				/* ----- Delay Time Knob ----- */
				currentXPos = xOffset  + (sliderWidth/2 - knobWidth);
				knob = EZKnob(
					parent: window,
					bounds: Rect(currentXPos, currentYPos, knobWidth, knobHeight),
					label: "Delay Time",
					controlSpec: ControlSpec.new(minval: ~minDelayTime, maxval: ~maxDelayTime, warp: \lin),
					action: {arg thisKnob; feedbackDelayLine.set(\delayTime, thisKnob.value)},
					initVal: ~minDelayTime,
					initAction: false,
					labelWidth: 60,
					// knobSize: an instance of Point,
					unitWidth: 0,
					labelHeight: 20,
					layout: \vert2,
					// gap: an instance of Point,
					margin: margin
				);
				knob.font = Font(~fontName, 11);
				/* ----- Feedback Amount Knob ----- */
				currentXPos = currentXPos + knobWidth;
				knob = EZKnob(
					parent: window,
					bounds: Rect(currentXPos, currentYPos, knobWidth, knobHeight),
					label: "Feedback",
					controlSpec: ControlSpec.new(minval: 0.0, maxval: 2.0, warp: \lin),
					action: {arg thisKnob; feedbackDelayLine.set(\feedbackAmount, thisKnob.value)},
					initVal: 0.0,
					initAction: false,
					labelWidth: 60,
					// knobSize: an instance of Point,
					unitWidth: 0,
					labelHeight: 20,
					layout: \vert2,
					// gap: an instance of Point,
					margin: margin
				);
				knob.font = Font(~fontName, 11);

				/* ----- Fifth Line ----- */
				/* ----- Delay Mode Selection Button ----- */
				currentYPos = currentYPos + knobHeight + 30;
				button = Button.new(
					parent: window,
					bounds: Rect(currentXPos - (buttonWidth/2) - 2.5, currentYPos, buttonWidth, buttonHeight));
				button.states = [["Normal", Color.black], ["Pitch Feedback", Color.black], ["Cross Feedback", Color.black]];
				button.action = ({ arg me;
					~modeSelectionBuses[index].set(me.value);
				});
				button.font = Font(~fontName, 11);

			});

			window.front;
			window.onClose_({
				x.free;
				o.free;
				voiceChannelsGroup.freeAll;
				outputMixer.free;
				~oscNetAddrProcessing.disconnect;
				~oscNetAddrTouchOSC.disconnect;
				Server.killAll;
				SerialPort.closeAll; });


			/* ----- Harmonizer - Touch OSC comunication ----- */

			/* ----- Default Parameters ----- */
			~oscNetAddrTouchOSC.sendMsg("/master/gain", 1);
			~oscNetAddrTouchOSC.sendMsg("/master/dryWet", 0.5);
			~oscNetAddrTouchOSC.sendMsg("/harmonizer/pitchShifter/keyLabel", ~keysLabels.at(0));
			~oscNetAddrTouchOSC.sendMsg("/harmonizer/pitchShifter/scaleLabel", ~scalesLabels.at(0));
			~oscNetAddrTouchOSC.sendMsg("/harmonizer/pitchShifter/formantRatio", 1);
			~oscNetAddrTouchOSC.sendMsg("/harmonizer/pitchShifter/grainsPeriod", 2);
			~oscNetAddrTouchOSC.sendMsg("/harmonizer/pitchShifter/timeDispersion", 1);
			~oscNetAddrTouchOSC.sendMsg("/reverb/dryWet", 0.5);
			~oscNetAddrTouchOSC.sendMsg("/reverb/roomSize", 0.5);
			~oscNetAddrTouchOSC.sendMsg("/reverb/highDamp", 0.5);


			/* ----- Master ----- */
			/* ----- Gain ----- */
			OSCdef.new(
				\masterGain,
				{
					arg msg;
					var val;
					val = msg[1];
					outputMixer.set(\master, val);
					~oscNetAddrTouchOSC.sendMsg("/master/gain", val.round(0.01));
					msg.postln;
				},
				'/master/gain'
			);
			/* ----- Dry/Wet ----- */
			OSCdef.new(
				\masterDryWet,
				{
					arg msg;
					var val;
					val = msg[1];
					outputMixer.set(\wet, val);
					~oscNetAddrTouchOSC.sendMsg("/master/dryWet", val.round(0.01));
					msg.postln;
				},
				'/master/dryWet'
			);

			/* ----- Reverb ----- */
			/* ----- Dry/Wet ----- */
			OSCdef.new(
				\reverbDryWet,
				{
					arg msg;
					var val;
					val = msg[1];
					outputMixer.set(\reverbMix, val);
					~oscNetAddrTouchOSC.sendMsg("/reverb/dryWet", val.round(0.01));
					msg.postln;
				},
				'/reverb/dryWet'
			);
			/* ----- Room Dimension ----- */
			OSCdef.new(
				\reverbRoomSize,
				{
					arg msg;
					var val;
					val = msg[1];
					outputMixer.set(\roomDimension, val);
					~oscNetAddrTouchOSC.sendMsg("/reverb/roomSize", val.round(0.01));
					msg.postln;
				},
				'/reverb/roomSize'
			);
			/* ----- Reverb High Damps ----- */
			OSCdef.new(
				\reverbHighDampValue,
				{
					arg msg;
					var val;
					val = msg[1];
					outputMixer.set(\reverbHighDamp, val);
					~oscNetAddrTouchOSC.sendMsg("/reverb/highDamp", val.round(0.01));
					msg.postln;
				},
				'/reverb/highDamp'
			);

			/* ----- Pitch Shifter ----- */
			/* ----- Key Selection ----- */
			OSCdef.new(
				\keySelection,
				{
					arg msg;
					var currentPos, currentScaleFreqs;
					if (~keyNum == 11,
						{ ~keyNum = 0; },
						{ ~keyNum = ~keyNum + 1; }
					);
					~keyControlBus.set(~keyNum);
					currentPos = ~keyNum;
					~currentKey = ~keyNum;
					currentScaleFreqs = Array.fill(~scales[~currentScale].size, {arg i;
						var note;
						// pull the freq for the current note
						note = ~chromaticFreqs.wrapAt(currentPos);
						// move to the next note for next time
						currentPos = currentPos + ~scales[~currentScale].at(i);
						note;
					});
					~rootIndexControlBus.set(currentScaleFreqs.detectIndex({
							arg item, i;
							item == ~chromaticFreqs.at(~serialMidiNote % 12);
						})
					);
					~oscNetAddrTouchOSC.sendMsg("/harmonizer/pitchShifter/keyLabel", ~keysLabels.at(~keyNum));
					("Key:" + ~keysLabels.at(~keyNum)).postln;
				},
				'/harmonizer/pitchShifter/key'
			);
			/* ----- Scale Selection ----- */
			OSCdef.new(
				\scaleSelection,
				{
					arg msg;
					var currentPos, currentScaleFreqs;
					if (~scaleNum == 16,
						{ ~scaleNum = 0; },
						{ ~scaleNum = ~scaleNum + 1; }
					);
					currentPos = ~currentKey;
					~scaleControlBus.set(~scaleNum);
					~currentScale = ~scaleNum;
					currentScaleFreqs = Array.fill(~scales[~scaleNum].size, {arg i;
						var note;
						// pull the freq for the current note
						note = ~chromaticFreqs.wrapAt(currentPos);
						// move to the next note for next time
						currentPos = currentPos + ~scales[~scaleNum].at(i);
						note;
					});
					~rootIndexControlBus.set(currentScaleFreqs.detectIndex({
							arg item, i;
							item == ~chromaticFreqs.at(~serialMidiNote % 12);
						})
					);





					~oscNetAddrTouchOSC.sendMsg("/harmonizer/pitchShifter/scaleLabel", ~scalesLabels.at(~scaleNum));
					("Scale:" + ~scalesLabels.at(~scaleNum)).postln;
				},
				'/harmonizer/pitchShifter/scale'
			);
			/* ----- Formant Ratio ----- */
			OSCdef.new(
				\formantRatio,
				{
					arg msg;
					var val;
					val = msg[1].round(0.01);
					~formantRatioControlBus.set(val);
					~oscNetAddrTouchOSC.sendMsg("/harmonizer/pitchShifter/formantRatio", val);
					("Formant Ratio:" + val).postln;
				},
				'/harmonizer/pitchShifter/formantRatio'
			);
			/* ----- Grain Period ----- */
			OSCdef.new(
				\grainsPeriod,
				{
					arg msg;
					var val;
					val = msg[1].round(2);
					~grainsPeriodControlBus.set(val);
					~oscNetAddrTouchOSC.sendMsg("/harmonizer/pitchShifter/grainsPeriod", val);
					("Grains Period:" + val).postln;
				},
				'/harmonizer/pitchShifter/grainsPeriod'
			);
			/* ----- Time Dispersion ----- */
			OSCdef.new(
				\timeDispersion,
				{
					arg msg;
					var val;
					val = msg[1];
					~pitchShifTimeDispersionControlBus.set(val);
					~oscNetAddrTouchOSC.sendMsg("/harmonizer/pitchShifter/timeDispersion", val.round(0.01));
					msg.postln;
				},
				'/harmonizer/pitchShifter/timeDispersion'
			);

			/* ----- Voices ----- */
			voiceChannels.do({ arg voiceChannel, index;

				var title, pitchRatioManager, pitchShifter, feedbackDelayLine, pitchfbModeCheckbox, crossfbModeCheckbox;

				pitchRatioManager = voiceChannel[0];
				pitchShifter = voiceChannel[1];
				feedbackDelayLine = voiceChannel[2];

				/* ----- Default parameters ----- */
				~oscNetAddrTouchOSC.sendMsg("/harmonizer/voice" ++ (index + 1) ++ "/gain", 0);
				~oscNetAddrTouchOSC.sendMsg("/harmonizer/voice" ++ (index + 1) ++ "/pan", 0);
				~oscNetAddrTouchOSC.sendMsg("/harmonizer/voice" ++ (index + 1) ++ "/octavesNumberLabel", 0);
				~oscNetAddrTouchOSC.sendMsg("/harmonizer/voice" ++ (index + 1) ++ "/upDownLabel", "Up");
				~oscNetAddrTouchOSC.sendMsg("/harmonizer/voice" ++ (index + 1) ++ "/voiceInterval", 1);
				~oscNetAddrTouchOSC.sendMsg("/harmonizer/voice" ++ (index + 1) ++ "/delayTime", 0);
				~oscNetAddrTouchOSC.sendMsg("/harmonizer/voice" ++ (index + 1) ++ "/feedbackAmount", 0);
				~oscNetAddrTouchOSC.sendMsg("/harmonizer/voice" ++ (index + 1) ++ "/feedbackModeLabel", "Normal Feedback");

				/* ----- Gain ----- */
				OSCdef.new(
					("gainVoice" ++ (index + 1)).asSymbol,
					{
						arg msg;
						var val;
						val = msg[1];
						pitchShifter.set(\gain, val);
						~oscNetAddrTouchOSC.sendMsg("/harmonizer/voice" ++ (index + 1) ++ "/gain", val.round(0.01));
						msg.postln;
					},
					("/harmonizer/voice" ++ (index + 1) ++ "/gain").asSymbol;
				);

				/* ----- Stereo Pan ----- */
				OSCdef.new(
					("panVoice" ++ (index + 1)).asSymbol,
					{
						arg msg;
						var val;
						val = msg[1];
						~stereoPanControlBuses[index].set(val);
						~oscNetAddrTouchOSC.sendMsg("/harmonizer/voice" ++ (index + 1) ++ "/pan", val.round(0.01));
						msg.postln;
					},
					("/harmonizer/voice" ++ (index + 1) ++ "/pan").asSymbol;
				);

				/* ----- Octave Number ----- */
				OSCdef.new(
					("voice" ++ (index + 1) ++ "OctavesNumber").asSymbol,
					{
						arg msg;
						if (~octavesNum == 2,
							{ ~octavesNum = 0; },
							{ ~octavesNum = ~octavesNum + 1; }
						);
						~octaveNumberBuses[index].set(~octavesNum);
						~oscNetAddrTouchOSC.sendMsg("/harmonizer/voice" ++ (index + 1) ++ "/octavesNumberLabel", ~octavesNum);
						("Number of octaves:" + ~octavesNum).postln;
					},
					("/harmonizer/voice" ++ (index + 1) ++ "/octavesNumber").asSymbol;
				);

				/* ----- Octave Up/Down ----- */
				OSCdef.new(
					("voice" ++ (index + 1) ++ "upDown").asSymbol,
					{
						arg msg;
						if (~upDownNum == 1,
							{ ~upDownNum = 0; },
							{ ~upDownNum = ~upDownNum + 1; }
						);
						~octaveUpDownBuses[index].set(~upDownNum);
						~oscNetAddrTouchOSC.sendMsg("/harmonizer/voice" ++ (index + 1) ++ "/upDownLabel", ~upDownLabels.at(~upDownNum));
						if ( ~upDownNum == 0,
							{ ("Upward Pitch Shifting").postln; },
							{ ("Downward Pitch Shifting").postln; }
						);
					},
					("/harmonizer/voice" ++ (index + 1) ++ "/upDown").asSymbol;
				);

				/* ----- Voice Interval ----- */
				OSCdef.new(
					("voice" ++ (index + 1) ++ "Interval").asSymbol,
					{
						arg msg;
						var val;
						val = msg[1].round;
						~voiceIntervalBusses[index].set(val - 1);
						~oscNetAddrTouchOSC.sendMsg("/harmonizer/voice" ++ (index + 1) ++ "/voiceInterval", (val));
						("Voice" + (index + 1) + "Interval:" + (val)).postln;
					},
					("/harmonizer/voice" ++ (index + 1) ++ "/voiceInterval").asSymbol;
				);

				/* ----- Delay Time ----- */
				OSCdef.new(
					("delayTimeVoice" ++ (index + 1)).asSymbol,
					{
						arg msg;
						var val;
						val = msg[1];
						feedbackDelayLine.set(\delayTime, val);
						~oscNetAddrTouchOSC.sendMsg("/harmonizer/voice" ++ (index + 1) ++ "/delayTime", val.round(0.01));
						msg.postln;
					},
					("/harmonizer/voice" ++ (index + 1) ++ "/delayTime").asSymbol;
				);

				/* ----- Delay Feedback ----- */
				OSCdef.new(
					("feedbackAmountVoice" ++ (index + 1)).asSymbol,
					{
						arg msg;
						var val;
						val = msg[1];
						feedbackDelayLine.set(\feedbackAmount, val);
						~oscNetAddrTouchOSC.sendMsg("/harmonizer/voice" ++ (index + 1) ++ "/feedbackAmount", val.round(0.01));
						msg.postln;
					},
					("/harmonizer/voice" ++ (index + 1) ++ "/feedbackAmount").asSymbol;
				);

				/* ----- Delay Feedback Mode ----- */
				OSCdef.new(
					("voice" ++ (index + 1) ++ "FeedbackMode").asSymbol,
					{
						arg msg;
						if (~feedbackModeNum == 2,
							{ ~feedbackModeNum = 0; },
							{ ~feedbackModeNum = ~feedbackModeNum + 1; }
						);
						~modeSelectionBuses[index].set(~feedbackModeNum);
						~oscNetAddrTouchOSC.sendMsg("/harmonizer/voice" ++ (index + 1) ++ "/feedbackModeLabel", ~feedbackModesLabels.at(~feedbackModeNum));
					},
					("/harmonizer/voice" ++ (index + 1) ++ "/feedbackMode").asSymbol;
				);
			});

			/* ----- Touch OSC comunication - Synth Pad Controls ----- */

			/* ----- Default Parameters ----- */
			~oscNetAddrTouchOSC.sendMsg("/synthPad/synth1/gain", 1);
			~oscNetAddrTouchOSC.sendMsg("/synthPad/synth2/gain", 0.12);
			~oscNetAddrTouchOSC.sendMsg("/synthPad/filters/lowPass/cutOffFrequency", 4000);
			~oscNetAddrTouchOSC.sendMsg("/synthPad/filters/lowPass/resonance", 0);
			~oscNetAddrTouchOSC.sendMsg("/synthPad/filters/highPass/cutOffFrequency", 100);
			~oscNetAddrTouchOSC.sendMsg("/synthPad/filters/highPass/resonance", 0);
			~oscNetAddrTouchOSC.sendMsg("/synthPad/lfo/amplitude", 0);
			~oscNetAddrTouchOSC.sendMsg("/synthPad/lfo/frequency", 0);
			~oscNetAddrTouchOSC.sendMsg("/synthPad/envelope/attack", 0.1);
			~oscNetAddrTouchOSC.sendMsg("/synthPad/envelope/decay", 0.05);
			~oscNetAddrTouchOSC.sendMsg("/synthPad/envelope/sustain", 0.5);
			~oscNetAddrTouchOSC.sendMsg("/synthPad/envelope/release", 1.0);

			/* ----- Wavetables ----- */
			/* ----- Sinusoidal Table Gain ----- */
			OSCdef.new(
				\synth1Gain,
				{
					arg msg;
					var val;
					val = msg[1];
					synth.set(\sinTableGain, val);
					~oscNetAddrTouchOSC.sendMsg("/synthPad/synth1/gain", val.round(0.01));
					msg.postln;
				},
				'/synthPad/synth1/gain'
			);
			/* ----- Chebyshev Table Gain ----- */
			OSCdef.new(
				\synth2Gain,
				{
					arg msg;
					var val;
					val = msg[1];
					synth.set(\chebTableGain, val);
					~oscNetAddrTouchOSC.sendMsg("/synthPad/synth2/gain", val.round(0.01));
					msg.postln;
				},
				'/synthPad/synth2/gain'
			);

			/* ----- LP Filter ----- */
			/* ----- CutOff Frequency ----- */
			OSCdef.new(
				\lowPassFilterCutOff,
				{
					arg msg;
					var val;
					val = msg[1];
					outputMixer.set(\lpfCutoff, val);
					~oscNetAddrTouchOSC.sendMsg("/synthPad/filters/lowPass/cutOffFrequency", val.round(1));
					msg.postln;
				},
				'/synthPad/filters/lowPass/cutOffFrequency'
			);
			/* ----- Resonance ----- */
			OSCdef.new(
				\lowPassFilterResonance,
				{
					arg msg;
					var val;
					val = msg[1];
					outputMixer.set(\lpfResonance, val);
					~oscNetAddrTouchOSC.sendMsg("/synthPad/filters/lowPass/resonance", val.round(0.01));
					msg.postln;
				},
				'/synthPad/filters/lowPass/resonance'
			);

			/* ----- HP Filter ----- */
			/* ----- CutOff Frequency ----- */
			OSCdef.new(
				\highPassFilterCutOff,
				{
					arg msg;
					var val;
					val = msg[1];
					outputMixer.set(\hpfCutoff, val);
					~oscNetAddrTouchOSC.sendMsg("/synthPad/filters/highPass/cutOffFrequency", val.round(1));
					msg.postln;
				},
				'/synthPad/filters/highPass/cutOffFrequency'
			);
			/* ----- Resonance ----- */
			OSCdef.new(
				\highPassFilterResonance,
				{
					arg msg;
					var val;
					val = msg[1];
					outputMixer.set(\hpfResonance, val);
					~oscNetAddrTouchOSC.sendMsg("/synthPad/filters/highPass/resonance", val.round(0.01));
					msg.postln;
				},
				'/synthPad/filters/highPass/resonance'
			);

			/* ----- LFO ----- */
			/* ----- Frequency ----- */
			OSCdef.new(
				\lfoFrequency,
				{
					arg msg;
					var val;
					val = msg[1];
					outputMixer.set(\lfoFreq, val);
					~oscNetAddrTouchOSC.sendMsg("/synthPad/lfo/frequency", val.round(0.01));
					msg.postln;
				},
				'/synthPad/lfo/frequency'
			);
			/* ----- Amplitude ----- */
			OSCdef.new(
				\lfoAmpplitude,
				{
					arg msg;
					var val;
					val = msg[1];
					outputMixer.set(\lfoAmp, val);
					~oscNetAddrTouchOSC.sendMsg("/synthPad/lfo/amplitude", val.round(0.01));
					msg.postln;
				},
				'/synthPad/lfo/amplitude'
			);

			/* ----- Envelope ----- */
			/* ----- Attack ----- */
			OSCdef.new(
				\synthAttack,
				{
					arg msg;
					var val;
					val = msg[1];
					outputMixer.set(\adsrA, val);
					~oscNetAddrTouchOSC.sendMsg("/synthPad/envelope/attack", val.round(0.01));
					msg.postln;
				},
				'/synthPad/envelope/attack'
			);
			/* ----- Decay ----- */
			OSCdef.new(
				\synthDecay,
				{
					arg msg;
					var val;
					val = msg[1];
					outputMixer.set(\adsrD, val);
					~oscNetAddrTouchOSC.sendMsg("/synthPad/envelope/decay", val.round(0.01));
					msg.postln;
				},
				'/synthPad/envelope/decay'
			);
			/* ----- Sustain ----- */
			OSCdef.new(
				\synthSustain,
				{
					arg msg;
					var val;
					val = msg[1];
					outputMixer.set(\adsrS, val);
					~oscNetAddrTouchOSC.sendMsg("/synthPad/envelope/sustain", val.round(0.01));
					msg.postln;
				},
				'/synthPad/envelope/sustain'
			);
			/* -----Release ----- */
			OSCdef.new(
				\synthRelease,
				{
					arg msg;
					var val;
					val = msg[1];
					outputMixer.set(\adsrR, val);
					~oscNetAddrTouchOSC.sendMsg("/synthPad/envelope/release", val.round(0.01));
					msg.postln;
				},
				'/synthPad/envelope/release'
			);
		});
	);
});

/* ----- UGen Graphs -----
Draws the SynthDefs UGen Graphs with the sc3-dot Quark.
Graphs are saved in SVG format in the assets/graphs directory. */
/*(
Dot.directory = thisProcess.nowExecutingPath.dirname +/+ "assets/graphs";
Dot.fontSize = 16; /* default = 10 */
Dot.useSplines = true; /* default = false */
Dot.drawInputName = true; /* default = false */
Dot.useTables = true; /* default = true */
Dot.renderMode = 'svg'; // run dot to generate a .pdf file and view that

a.draw; b.draw; c.draw; d.draw; e.draw; f.draw; g.draw;
);*/
