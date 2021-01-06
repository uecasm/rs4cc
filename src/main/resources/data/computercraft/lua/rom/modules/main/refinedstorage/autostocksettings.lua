local filename = ".autostock"
local config
local defaults = {
    refresh = 10,
    stock = {
        { item = {name="minecraft:sand", count=128} },
        { item = {name="minecraft:glass", count=128} },
        { item = {name="minecraft:stone_bricks", count=64} },
    },
}

local function copy(value)
    if type(value) ~= "table" then return value end
    local result = {}
    for k, v in pairs(value) do result[k] = copy(v) end
    return result
end

local function load()
    local reload = false
    if not _G.refinedstorage_autostock_config then
        _G.refinedstorage_autostock_config = {}
        reload = true
    end
    config = _G.refinedstorage_autostock_config

    if reload then
        local input = {}
        if fs.exists(filename) then
            local file = fs.open(filename, "r")
            input = textutils.unserialize(file.readAll())
            file.close()
        end
        for k,v in pairs(input) do
            config[k] = input[k]
        end
        for k,v in pairs(defaults) do
            if config[k] == nil then config[k] = copy(v) end
        end
    end
end

local function save()
    local file = fs.open(filename, "w") or error("Cannot open file", filename, 2)
    file.write(textutils.serialize(config))
    file.close()
end

local function get(name)
    return config[name]
end
local function set(name, value)
    config[name] = value
    save()
end

local function getRefresh() return get("refresh") end
local function setRefresh(value) set("refresh", value) end

local function getStock() return get("stock") end
local function setStock(value) set("stock", value) end

local handlers = {
    item = {
        type = "item",
        getStack = function (entry) return entry.item end,
        makeEntry = function (stack) return {item=stack} end,
        getQuantity = function (stack) return stack.count end,
        setQuantity = function (stack, value) stack.count = value end,
        formatQuantity = function (quantity) return tostring(quantity) end,
        find = function (stack) return refinedstorage.getItem(stack, true, true) end,
        all = function () return refinedstorage.getItems() end,
        findPattern = function (stack) return refinedstorage.getPattern(stack) end,
        craft = function (stack, quantity) return refinedstorage.scheduleTask(stack, quantity) end,
    }, fluid = {
        type = "fluid",
        getStack = function (entry) return entry.fluid end,
        makeEntry = function (stack) return {fluid=stack} end,
        getQuantity = function (stack) return stack.amount end,
        setQuantity = function (stack, value) stack.amount = value * 1000 end,
        formatQuantity = function (quantity) return string.format("%.1fB", quantity / 1000) end,
        find = function (stack) return refinedstorage.getFluid(stack, true, true) end,
        all = function () return refinedstorage.getFluids() end,
        findPattern = function (stack) return refinedstorage.getFluidPattern(stack) end,
        craft = function (stack, quantity) return refinedstorage.scheduleFluidTask(stack, quantity) end,
    },
}

local function getHandlers(entry)
    if entry.item then return handlers.item, handlers.item.getStack(entry) end
    if entry.fluid then return handlers.fluid, handlers.fluid.getStack(entry) end
end

load()

return {
    getRefresh = getRefresh, setRefresh = setRefresh,
    getStock = getStock, setStock = setStock,
    getHandlers = getHandlers, handlers = handlers,
}
