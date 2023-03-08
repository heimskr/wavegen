`timescale 1 ps / 1 ps

module top (
	input  clk,
	input  btnc,
	input  btnd,
	input  btnl,
	input  btnr,
	input  btnu,
	input  cpu_resetn,
	output [7:0] led,
	input  [7:0] sw,
	output ac_mclk,
	input  ac_adc_sdata,
	output ac_dac_sdata,
	output ac_bclk,
	output ac_lrclk,
	inout  scl,
	inout  sda,
	output hdmi_tx_cec,     // CE control bidirectional
	input  hdmi_tx_hpd,     // hot-plug detect
	output hdmi_tx_rscl,    // DDC bidirectional
	output hdmi_tx_rsda,    // DDC bidirectional
	output hdmi_tx_clk_n,   // HDMI clock differential negative
	output hdmi_tx_clk_p,   // HDMI clock differential positive
	output [2:0] hdmi_tx_n, // Three HDMI channels differential negative
	output [2:0] hdmi_tx_p, // Three HDMI channels differential positive,
	inout  [7:0] ja, // Pmod JA connector
	output uart_rx_out,
	input  uart_tx_in
);

	wire clk12MHz;
	wire clk30MHz;
	wire clk50MHz;

	clk_wiz_0 clk_0 (
		.clk(clk),
		.reset(!cpu_resetn),
		.clk12(clk12MHz),
		.clk30(clk30MHz),
		.clk50(clk50MHz)
	);

	wire clk_pix1;
	wire clk_pix5;

	clk_wiz_hdmi clk_hdmi (
		.clk(clk),
		.reset(!cpu_resetn),
		.clk_pix1(clk_pix1),
		.clk_pix5(clk_pix5)
	);

	// wire clk_gbx4;
	// wire clk_nesx16;

	// clk_wiz_cpu clk_cpu (
	// 	.clk(clk),
	// 	.reset(!cpu_resetn),
	// 	.clk_gbx4(clk_gbx4),
	// 	.clk_nesx16(clk_nesx16)
	// );

	// reg [1:0] counter_gb;
	// reg clk_gb_slow;
	// reg [1:0] clk_gb_state; // (0 or 2) -> armed, 1 -> firing, 3 -> disarmed
	// wire clk_gb_fast;
	// wire clk_gb_buf;

	// always @(posedge clk_gbx4) begin
	// 	if (counter_gb == 2'd3) begin
	// 		counter_gb  <= 2'd0;
	// 		clk_gb_slow <= 1'b1;
	// 	end else begin
	// 		counter_gb  <= counter_gb + 2'd1;
	// 		clk_gb_slow <= 1'b0;
	// 	end
	// end

	// always @(posedge clk) begin
	// 	if (!cpu_resetn) begin
	// 		clk_gb_state <= 2'd0;
	// 	end else if (!clk_gb_state[0]) begin
	// 		if (clk_gb_slow) begin
	// 			clk_gb_state <= 2'd1;
	// 		end
	// 	end else if (clk_gb_state == 2'd1) begin
	// 		clk_gb_state <= 2'd3;
	// 	end else begin
	// 		if (!clk_gb_slow) begin
	// 			clk_gb_state <= 2'd0;
	// 		end
	// 	end
	// end

	// assign clk_gb_fast = clk_gb_state == 2'd1;
	// BUFG gb_bufg (.I(clk_gb_fast), .O(clk_gb_buf));

	// reg [3:0] counter_nes;
	// reg clk_nes_slow;
	// reg [1:0] clk_nes_state; // See above
	// wire clk_nes_fast;
	// wire clk_nes_buf;

	// always @(posedge clk_nesx16) begin
	// 	if (counter_nes == 4'd15) begin
	// 		counter_nes  <= 4'd0;
	// 		clk_nes_slow <= 1'b1;
	// 	end else begin
	// 		counter_nes  <= counter_nes + 4'd1;
	// 		clk_nes_slow <= 1'b0;
	// 	end
	// end

	// always @(posedge clk) begin
	// 	if (!cpu_resetn) begin
	// 		clk_nes_state <= 2'd0;
	// 	end else if (!clk_nes_state[0]) begin
	// 		if (clk_nes_slow) begin
	// 			clk_nes_state <= 2'd1;
	// 		end
	// 	end else if (clk_nes_state == 2'd1) begin
	// 		clk_nes_state <= 2'd3;
	// 	end else begin
	// 		if (!clk_nes_slow) begin
	// 			clk_nes_state <= 2'd0;
	// 		end
	// 	end
	// end

	// assign clk_nes_fast = clk_nes_state == 2'd1;
	// BUFG nes_bufg (.I(clk_nes_fast), .O(clk_nes_buf));

	assign ac_mclk = clk12MHz;

	wire dbu;
	wire dbr;
	wire dbd;
	wire dbl;
	wire dbc;

	audio_init initialize_audio (
		.clk(clk50MHz),
		.rst(!cpu_resetn),
		.sda(sda),
		.scl(scl)
	);

	Debouncer5 dbuttons (
		.clock(clk),
		.reset(!cpu_resetn),
		.io_in_0(btnu),
		.io_in_1(btnr),
		.io_in_2(btnl),
		.io_in_3(btnd),
		.io_in_4(btnc),
		.io_out_0(dbu),
		.io_out_1(dbr),
		.io_out_2(dbl),
		.io_out_3(dbd),
		.io_out_4(dbc)
	);

	wire [23:0] out_audioL;
	wire [23:0] out_audioR;

	wire [17:0] rom_addr_gb;
	wire [23:0] rom_out_gb;

	blk_mem_gen_0 gb_rom (
		.clka(clk),
		.addra(rom_addr_gb),
		.douta(rom_out_gb)
	);

	wire [16:0] rom_addr_nes;
	wire [23:0] rom_out_nes;

	NesROM nes_rom (
		.clka(clk),
		.addra(rom_addr_nes),
		.douta(rom_out_nes)
	);

	wire [7:0] rx_byte;
	wire rx_ready;
	wire rx_error;
	reg rx_ack;
	wire [7:0] tx_byte;
	wire tx_ready;

	always @(posedge clk) begin
		rx_ack <= cpu_resetn & rx_ready;
	end

	UART #(.FREQ(100_000_000)) uart_thing (
		.reset(cpu_resetn),
		.clk(clk),
		.rx_i(uart_tx_in ^ sw[0]),
		.rx_data_o(rx_byte),
		.rx_ready_o(rx_ready),
		.rx_ack_i(rx_ack),
		.rx_error_o(rx_error),
		.tx_o(uart_rx_out),
		.tx_data_i(tx_byte),
		.tx_ready_i(tx_ready),
		.tx_ack_o()
	);

	wire nes_a;
	wire nes_b;
	wire nes_select;
	wire nes_start;
	wire nes_up;
	wire nes_down;
	wire nes_left;
	wire nes_right;
	wire use_nes;

	wire use_nes_in;
	wire use_nes_in_valid;

	MainBoth main_module_both (
		.clock(clk),
		.reset(!cpu_resetn),
		.io_pixClock(clk_pix1),
		// .io_clockNES(clk_nes_buf),
		// .io_clockGB(clk_gb_buf),
		.io_clockNES(1'b0),
		.io_clockGB(1'b0),
		.io_pulseU(dbu),
		.io_pulseR(dbr),
		.io_pulseL(dbl),
		.io_pulseD(dbd),
		.io_pulseC(dbc),
		.io_sw(sw),
		.io_outL(out_audioL),
		.io_outR(out_audioR),
		.io_led(led),
		.io_addrGB(rom_addr_gb),
		.io_addrNES(rom_addr_nes),
		.io_romGB(rom_out_gb),
		.io_romNES(rom_out_nes),
		.io_jaIn(ja),
		.io_pulseOut(ja[3]),
		.io_latchOut(ja[2]),
		.io_rxByte_valid(rx_ready),
		.io_rxByte_bits(rx_byte),
		.io_txByte_valid(tx_ready),
		.io_txByte_bits(tx_byte),
		.io_nesButtons_a(nes_a),
		.io_nesButtons_b(nes_b),
		.io_nesButtons_select(nes_select),
		.io_nesButtons_start(nes_start),
		.io_nesButtons_up(nes_up),
		.io_nesButtons_down(nes_down),
		.io_nesButtons_left(nes_left),
		.io_nesButtons_right(nes_right),
		.io_useNES(use_nes),
		.io_useNESIn_valid(use_nes_in_valid),
		.io_useNESIn_bits(use_nes_in)
	);

	i2s_ctl audio_inout (
		.CLK_I(clk),    // Sys clk
		.RST_I(!cpu_resetn),    // Sys rst
		.EN_TX_I(1),  // Transmit Enable (push sound data into chip)
		.EN_RX_I(0), // Receive enable (pull sound data out of chip)
		// .FS_I(4'b0101), // Sampling rate selector
		.FS_I(4'b0000), // Sampling rate selector
		.MM_I(0),     // Audio controller Master mode select
		.D_L_I(out_audioL),    // Left channel data input from mix (mixed audio output)
		.D_R_I(out_audioR),   // Right channel data input from mix
		.D_L_O(in_audioL),    // Left channel data (input from mic input)
		.D_R_O(in_audioR),    // Right channel data (input from mic input)
		.BCLK_O(ac_bclk),   // serial CLK
		.LRCLK_O(ac_lrclk),  // channel CLK
		.SDATA_O(ac_dac_sdata),  // Output serial data
		.SDATA_I(ac_adc_sdata)   // Input serial data
	);

	reg [23:0] storedL;
	reg [23:0] storedR;

	always @(posedge clk) begin
		storedL <= out_audioL;
		storedR <= out_audioR;
	end

	Display display (
		.clk(clk),
		.clk_pix1(clk_pix1),
		.clk_pix5(clk_pix5),
		.sw(sw),
		.buttonL(btnl),
		.buttonR(btnr),
		.clk30(clk30MHz),
		.rst_n(cpu_resetn),
		.hdmi_tx_cec(hdmi_tx_cec),
		.hdmi_tx_hpd(hdmi_tx_hpd),
		.hdmi_tx_rscl(hdmi_tx_rscl),
		.hdmi_tx_rsda(hdmi_tx_rsda),
		.hdmi_tx_clk_n(hdmi_tx_clk_n),
		.hdmi_tx_clk_p(hdmi_tx_clk_p),
		.hdmi_tx_n(hdmi_tx_n),
		.hdmi_tx_p(hdmi_tx_p),
		.audioL(storedL),
		.audioR(storedR),
		.nesA(nes_a),
		.nesB(nes_b),
		.nesSelect(nes_select),
		.nesStart(nes_start),
		.nesUp(nes_up),
		.nesDown(nes_down),
		.nesLeft(nes_left),
		.nesRight(nes_right),
		.useNES(use_nes),
		.useNESOutValid(use_nes_in_valid),
		.useNESOut(use_nes_in)
	);

endmodule
