_G.refinedstorage = peripheral.find("refinedstorage")

if refinedstorage then
    local completion = require "cc.shell.completion"

    shell.setPath(shell.path() .. ":/rom/programs/refinedstorage")
    help.setPath(help.path() .. ":/rom/help/refinedstorage")

    shell.setCompletionFunction("rom/programs/refinedstorage/stock.lua", completion.build(
        { completion.choice, { "list", "item ", "fluid ", "delete ", "refresh", "refresh ", "clean", "start" } }
    ))
end
