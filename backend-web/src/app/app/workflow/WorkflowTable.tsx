import {Table} from "antd"

export default async function WorkingTable({
  params,
}: {
  params: { workbenchId: string };
}) { 
    const {workbenchId} = await params;
    return (
        <div>
            <Table columns={[]} dataSource={[]} />
        </div>
    )
}