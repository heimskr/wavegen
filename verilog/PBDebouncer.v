// Credit: https://www.fpga4fun.com/Debouncer2.html

module PBDebouncer(
    input clk,
    input rst,
    input btn,  // "PB" is the glitchy, asynchronous to clk, active low push-button signal

    // from which we make three outputs, all synchronous to the clock
    output reg state, // 1 as long as the push-button is active (down)
    output down,      // 1 for one clock cycle when the push-button goes down (i.e. just pushed)
    output up         // 1 for one clock cycle when the push-button goes up (i.e. just released)
);

    // First use two flip-flops to synchronize the PB signal the "clk" clock domain
    reg PB_sync_0; always @(posedge clk) PB_sync_0 <= ~btn;  // invert PB to make PB_sync_0 active high
    reg PB_sync_1; always @(posedge clk) PB_sync_1 <= PB_sync_0;

    // Next declare a 16-bits counter
    reg [15:0] count;

    // When the push-button is pushed or released, we increment the counter
    // The counter has to be maxed out before we decide that the push-button state has changed

    wire idle = (state == PB_sync_1);
    wire count_max = &count;	// true when all bits of count are 1's

    always @(posedge clk) begin
        if (rst) begin
            count <= 0;
            state <= 0;
        end else if (idle) begin
            count <= 0;  // nothing's going on
        end else begin
            count <= count + 16'd1;  // something's going on, increment the counter
            if (count_max)
                state <= ~state;  // if the counter is maxed out, PB changed!
        end
    end

    assign down = ~idle & count_max & ~state;
    assign up   = ~idle & count_max &  state;

endmodule
