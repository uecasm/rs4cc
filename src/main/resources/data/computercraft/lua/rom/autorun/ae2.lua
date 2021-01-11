_G.ae2 = peripheral.find("ae2")

if ae2 then
    local completion = require "cc.shell.completion"

    shell.setPath(shell.path() .. ":/rom/programs/ae2")
    help.setPath(help.path() .. ":/rom/help/ae2")

    shell.setCompletionFunction("rom/programs/ae2/stock.lua", completion.build(
        { completion.choice, { "list", "item ", "fluid ", "delete ", "move ", "refresh", "refresh ", "clean", "start" } }
    ))
end
